"""仅面向显式隔离环境的真实HTTP验证和负载采样，不访问开发库。"""
import argparse
import concurrent.futures as cf
import http.client
import json
import math
import io
import subprocess
import socket
from pathlib import Path
import sys
import threading
import time
from urllib.parse import urlencode

P=argparse.ArgumentParser()
P.add_argument('--workdir', required=True)
P.add_argument('--mode', choices=['default','behavior','profile','load','soak','arrival','serial','disconnect','recovery','fault','media'], required=True)
P.add_argument('--scale', type=int, default=10000)
P.add_argument('--duration', type=int, default=20)
P.add_argument('--label', default='')
A=P.parse_args()
ROOT=Path(A.workdir).resolve()
ENV=json.loads((ROOT/'environment.json').read_text(encoding='utf-8'))
if ENV.get('schema')!='nebula_deep_20261107' or ENV.get('redisPort')!=16379 or ENV.get('applicationPort')!=18080:
    raise SystemExit('必须使用本轮明确隔离的数据库、Redis和后端端口')
FIX=json.loads((ROOT/f'fixture-{A.scale}.json').read_text(encoding='utf-8'))
OWN_PUBLIC=sum(i%39==0 and i%10!=0 and i%37!=0 for i in range(A.scale))
OWN_DRAFT=sum(i%39==0 and i%10==0 and i%37!=0 for i in range(A.scale))
OUT=ROOT/f'{A.mode}-{A.scale}-{time.time_ns()}'
OUT.mkdir()
TLS=threading.local()
LOCK=threading.Lock()
RAW=(OUT/'requests.jsonl').open('a',encoding='utf-8')
RESULT={'mode':A.mode,'label':A.label,'scale':A.scale,'scope':'loopback-http','latencyBasis':'closed-loop统计校验通过响应的延迟；fixed-arrival另计全部响应的排队与完成时间','checks':[],'stages':[]}
TOKENS={}

def save():
    (OUT/'result.json').write_text(json.dumps(RESULT,ensure_ascii=False,indent=2),encoding='utf-8')

def request(path, actor=1, method='GET', body=None, raw=False, timeout=15, extra_headers=None):
    conn=getattr(TLS,'connection',None)
    if conn is None:
        conn=http.client.HTTPConnection('127.0.0.1',18080,timeout=timeout);TLS.connection=conn
    conn.timeout=timeout
    headers={'Accept':'application/json'}
    if actor in TOKENS:headers['Authorization']='Bearer '+TOKENS[actor]
    if extra_headers:headers.update(extra_headers)
    if body is not None:headers['Content-Type']='application/json';body=json.dumps(body,ensure_ascii=False).encode()
    start=time.perf_counter()
    try:
        conn.request(method,path,body=body,headers=headers)
        res=conn.getresponse();data=res.read();status=res.status;rh={k.lower():v for k,v in res.getheaders()}
        ms=(time.perf_counter()-start)*1000
        parsed=json.loads(data) if data and not raw else data
        return status,parsed,rh,ms,len(data)
    except Exception:
        conn.close();TLS.connection=None;raise

def ok(path, actor=1, method='GET', body=None):
    status,data,h,ms,n=request(path,actor,method,body)
    assert status==200 and data.get('code')==200, f'{path}: http={status},code={data.get("code")}'
    return data['data']

def check(name, fn):
    start=time.perf_counter()
    try:
        detail=fn();assert detail is not False, '业务条件不成立';row={'name':name,'passed':True,'detail':detail}
    except Exception as e:
        row={'name':name,'passed':False,'error':type(e).__name__+': '+str(e)}
    row['seconds']=round(time.perf_counter()-start,3);RESULT['checks'].append(row);save()
    print(json.dumps(row,ensure_ascii=False),flush=True)

def login():
    password=(ROOT/'.private/login-password').read_text().strip()
    for actor in [0,1,2,11]:
        data=ok('/auth/login',actor=None,method='POST',body={'account':f'deep_actor_{actor}','password':password})
        TOKENS[actor]=data['token']

def valid_index(i):return 0<=i<A.scale and i%10!=0 and i%37!=0

def search_data(data):
    assert data['counts']['article']==FIX['expectedSearchArticle'], '文章命中总数与独立生成模型不符'
    assert data['counts']['comment']==FIX['expectedSearchComment'], '评论命中总数与独立生成模型不符'
    page=data['page']
    assert page['totalRow']==2*FIX['expectedVisibleArticles'], '搜索总数不符'
    for r in page['records']:
        assert r['type'] in ['article','comment']
        i=r['id']-(9000000 if r['type']=='article' else 19000000)
        assert valid_index(i), '搜索泄露草稿或删除文章的关联内容'
        assert r['targetId']==9000000+i
        assert not any(k in r for k in ['password','email','phone'])

def published_data(data):
    assert data['totalRow']==FIX['expectedVisibleArticles'], '已发布总数与独立模型不符'
    for r in data['records']:
        assert valid_index(r['id']-9000000),'已发布列表含草稿或删除记录'

def profile_data(data,own=True):
    info=data['userProfileInfoVO'];assert info['id']==8000001 and info['nid']=='deep_actor_1','个人主页返回了错误账号'
    assert 'password' not in info
    assert data['publicArticles']['totalRow']==OWN_PUBLIC
    for item in data['publicArticles']['records']:
        i=item['id']-9000000;assert valid_index(i) and i%39==0,'个人主页混入他人文章或非公开文章'
    assert data['comments']['totalRow']==FIX['expectedVisibleArticles']+FIX['hotReplies']
    for item in data['comments']['records']:assert valid_index(item['articleId']-9000000),'个人主页混入了草稿文章的评论'
    if own:
        assert isinstance(data['draftArticles'],dict) and data['draftArticles']['totalRow']==OWN_DRAFT
    else:
        # 隐私约束是没有返回他人的草稿，null和空页都不能包含任何草稿数据。
        drafts=data.get('draftArticles')
        assert drafts is None or drafts['totalRow']==0 and not drafts['records']
        assert not data['loginLogs'] and info.get('email') is None and info.get('phone') is None,'他人主页返回私人资料'

def profile_isolation():
    profile_data(ok('/api/profile/8000001'))
    profile_data(ok('/api/profile/8000001',actor=2),own=False)
    return {'publicArticles':OWN_PUBLIC,'selfDrafts':OWN_DRAFT,'publicComments':FIX['expectedVisibleArticles']+FIX['hotReplies'],'foreignPrivateFieldsDenied':True}

def publication_pages():
    expected=[9000000+i for i in range(A.scale-1,-1,-1) if valid_index(i)]
    seen=[]
    for page in range(1,21):
        data=ok('/articles/published?'+urlencode({'current':page,'size':17}));published_data(data)
        ids=[x['id'] for x in data['records']]
        assert ids==expected[(page-1)*17:page*17],f'第{page}页在时间相同的记录中发生遗漏、重复或顺序变化'
        seen.extend(ids)
    assert len(set(seen))==340
    filtered=ok('/articles/published?categoryIds=8000000&tagIds=8000000&size=17');published_data(filtered)
    assert [x['id'] for x in filtered['records']]==expected[:17]
    return {'pages':20,'distinctIds':len(set(seen)),'total':len(expected)}

def search_pages():
    seen=set();order=[];keys=[]
    for page in range(1,9):
        data=ok('/search?'+urlencode({'q':'deepneedle','current':page,'size':17}));search_data(data)
        for r in data['page']['records']:
            key=(r['type'],r['id']);assert key not in seen,'搜索翻页出现重复项';seen.add(key)
            keys.append(key)
            order.append((r['createTime'],r['type'],-r['id']))
    assert all(a[0]>=b[0] for a,b in zip(order,order[1:])), '搜索时间倒序不稳定'
    expected=[('comment',19000000+i) for i in range(A.scale-1,-1,-1) if valid_index(i)][:len(keys)]
    assert keys==expected,'搜索前八页与按生成时间独立计算的结果集合不符'
    return {'pages':8,'distinctItems':len(seen)}

def literal_search():
    for keyword in ['literal%_!','literal%_',"' OR 1=1 --",'unfindable-6de113a2']:
        data=ok('/search?'+urlencode({'q':keyword,'type':'article'}));records=data['page']['records']
        expected=1 if keyword.startswith('literal') else 0
        assert data['page']['totalRow']==expected and len(records)==expected, '通配符未按字面处理或搜索条件越界'
        if records:assert records[0]['id']==9000017
    return {'literalAndInjectionCases':4}

def boundaries():
    probes=[('/search?q='+('x'*51),400),('/search?current=0',400),('/search?size=51',400),('/search?type=bogus',400),('/articles/published?size=101',400),('/articles/published?current=0',400),('/comments/threads?articleId=9000001&size=101',400)]
    for path,expected in probes:
        s,d,*_=request(path);assert s==expected,f'{path}: expected {expected}, got {s}'
    for path in ['/articles/published','/search?q=deepneedle','/api/profile/8000001']:
        s,d,*_=request(path,actor=None);assert s==401,f'匿名访问未被拒绝:{path}:{s}'
    return {'probes':len(probes)+3}

def private_draft():
    # 编号10是生成模型中的草稿，属于11号账号；不依赖查询实现建立预期。
    s,d,*_=request('/articles/9000010',actor=1);assert s!=200 and 'deepneedle' not in json.dumps(d)
    s,d,*_=request('/articles/9000010/edit',actor=1);assert s!=200
    own=ok('/articles/9000010/edit',actor=11);assert own['id']==9000010 and own['status']==0
    return {'foreignReadAndEditDenied':True,'ownerCanEdit':True}

def reading_isolation():
    result=ok('/reading/9000001/flags',method='PUT',body={'favorite':True,'later':False,'userId':8000002})
    assert result['favorite'] is True
    other=ok('/reading/9000001',actor=2);assert not other['favorite'],'伪造userId写入了他人的书架'
    for _ in range(4):ok('/reading/9000001/flags',method='PUT',body={'favorite':True,'later':False})
    lib=ok('/reading?kind=favorite');assert sum(x['articleId']==9000001 for x in lib['records'])==1
    return {'ownerOnly':True,'retriesIdempotent':True}

def hot_comments():
    root=ok('/comments/threads?articleId=9000001&size=10')
    roots=[r for r in root['records'] if r['id']==19000001];assert len(roots)==1
    assert roots[0]['replyCount']==FIX['hotReplies'], '根评论回复总数与生成模型不符'
    first=ok('/comments/19000001/replies?size=19')
    assert first['totalRow']==FIX['hotReplies'] and len(first['records'])==19, '回复分页大小或总量不符'
    assert [r['id'] for r in first['records']]==list(range(30000000,30000019)), '回复首页排序不符'
    last=ok('/comments/19000001/replies?'+urlencode({'size':19,'currentPage':math.ceil(FIX['hotReplies']/19)}))
    assert last['records'][-1]['id']==30000000+FIX['hotReplies']-1, '回复末页遗漏数据'
    return {'replyCount':FIX['hotReplies'],'rootPageSize':len(root['records']),'replyPageSize':len(first['records'])}

def workspace_conflict():
    current=ok('/article-workspaces/new')['revision']
    def save_one(index):return request('/article-workspaces/new',method='PUT',body={'revision':current,'draft':{'title':f'竞争备份{index}','content':f'唯一正文{index}','categoryId':8000000,'tagIds':[],'sourceVersion':0}})
    with cf.ThreadPoolExecutor(max_workers=8) as pool:responses=list(pool.map(save_one,range(8)))
    success=[r for r in responses if r[0]==200]
    assert len(success)==1, f'相同旧版本应只有一个写入成功，实际{len(success)}'
    assert all(r[0] in [200,409] for r in responses),[(r[0],r[1].get('code')) for r in responses]
    after=ok('/article-workspaces/new');assert after['revision']==current+1
    other=ok('/article-workspaces/new',actor=2);assert other.get('draft') is None
    return {'writers':8,'committed':1,'conflicts':7,'ownerOnly':True}

def likes_concurrent():
    ok('/articles/9000001')
    with cf.ThreadPoolExecutor(max_workers=16) as pool:
        responses=list(pool.map(lambda _:request('/articles/like',method='PUT',body={'articleId':9000001,'userId':8000002}),range(96)))
    assert all(r[0]==200 and r[1]['code']==200 for r in responses),'点赞并发出现错误，不能将拒绝请求算作成功操作'
    values=[r[1]['data'] for r in responses];assert all(isinstance(x,bool) for x in values)
    assert sum(values)==48,'96次切换应各有48次点赞与取消'
    (OUT/'like-oracle.json').write_text(json.dumps({'articleId':9000001,'actorId':8000001,'spoofedUserId':8000002,'successfulToggles':96,'expectedFinalRelations':0,'expectedFinalCount':0}),encoding='utf-8')
    return {'successfulToggles':96,'expectedFinalRelations':0,'finalStorageVerification':'另由Flex独立读取并核对'}

def percentile(values,p):
    if not values:return None
    v=sorted(values);return round(v[min(len(v)-1,math.ceil(len(v)*p)-1)],2)

def load_stage(name,threads,duration):
    # 封闭循环负载：延迟包含读完整个响应的时间，不将拒绝量当成业务吞吐量。
    start=time.perf_counter();end=start+duration;rows=[]
    endpoints=[('/articles/published?size=20',published_data),('/articles/published?current=300&size=20',published_data),('/search?q=deepneedle&size=20',search_data),('/search?q=absent-86a87&type=all',lambda d: d['page']['totalRow']==0),('/comments/19000001/replies?size=20',lambda d: d['totalRow']==FIX['hotReplies']),('/api/profile/8000001',profile_data)]
    def worker(index):
        n=0;local=[]
        while time.perf_counter()<end:
            path,validate=endpoints[(index+n)%len(endpoints)];n+=1
            attempt_start=time.perf_counter()
            try:
                status,data,h,ms,size=request(path)
                valid=False;error=None
                if status==200 and data.get('code')==200:
                    outcome=validate(data['data']);assert outcome is not False
                    valid=True
                elif status!=429:error=f'http={status},code={data.get("code")}'
                item={'path':path,'http':status,'valid':valid,'ms':round(ms,3),'bytes':size,'error':error}
            except Exception as e:item={'path':path,'http':0,'valid':False,'ms':round((time.perf_counter()-attempt_start)*1000,3),'bytes':0,'error':type(e).__name__+':'+str(e)}
            local.append(item)
            with LOCK:RAW.write(json.dumps(item,ensure_ascii=False)+'\n')
        return local
    with cf.ThreadPoolExecutor(max_workers=threads) as pool:
        for local in pool.map(worker,range(threads)):rows.extend(local)
    elapsed=time.perf_counter()-start;lat=[r['ms'] for r in rows if r['valid']];status={str(s):sum(r['http']==s for r in rows) for s in sorted(set(r['http'] for r in rows))}
    summary={'name':name,'workers':threads,'durationSeconds':round(elapsed,3),'requests':len(rows),'validResponses':len(lat),'validRps':round(len(lat)/elapsed,2),'statuses':status,'validationOrTransportErrors':sum(bool(r['error']) for r in rows),'p50Ms':percentile(lat,.5),'p95Ms':percentile(lat,.95),'p99Ms':percentile(lat,.99),'maxMs':max(lat,default=None),'errorSamples':[r for r in rows if r['error']][:5]}
    for path,_ in endpoints:
        sample=[r for r in rows if r['path']==path and r['valid']]
        summary.setdefault('endpoints',[]).append({'path':path,'valid':len(sample),'p95Ms':percentile([r['ms'] for r in sample],.95)})
    RESULT['stages'].append(summary);RAW.flush();save();print(json.dumps(summary,ensure_ascii=False),flush=True)

def arrival_stage(rate,duration):
    """固定到达速率另计排队时间，补充封闭循环下客户端自然降速的观测盲区。"""
    beginning=time.perf_counter();futures=[];dropped=0
    def one(sequence,due):
        start=time.perf_counter()
        path,validate=[('/articles/published?size=20',published_data),('/search?q=deepneedle&size=20',search_data),('/comments/19000001/replies?size=20',lambda d:d['totalRow']==FIX['hotReplies'])][sequence%3]
        try:
            status,data,headers,ms,size=request(path)
            valid=False;error=None
            if status==200 and data.get('code')==200:assert validate(data['data']) is not False;valid=True
            elif status!=429:error=f'http={status},code={data.get("code")}'
        except Exception as e:status=0;valid=False;error=type(e).__name__+':'+str(e)
        item={'path':path,'http':status,'valid':valid,'error':error,'queueMs':round((start-due)*1000,3),'endToEndMs':round((time.perf_counter()-due)*1000,3)}
        with LOCK:RAW.write(json.dumps(item,ensure_ascii=False)+'\n')
        return item
    with cf.ThreadPoolExecutor(max_workers=16) as pool:
        for sequence in range(rate*duration):
            due=beginning+sequence/rate
            wait=due-time.perf_counter()
            if wait>0:time.sleep(wait)
            if sum(not f.done() for f in futures)>=32:dropped+=1;continue
            futures.append(pool.submit(one,sequence,due))
        rows=[f.result() for f in futures]
    elapsed=time.perf_counter()-beginning
    summary={'name':'fixed-arrival','offeredRps':rate,'offeredRequests':rate*duration,'offeringSeconds':duration,'durationSeconds':round(elapsed,3),'clientBacklogDrops':dropped,'workers':16,'requests':len(rows),'validResponses':sum(r['valid'] for r in rows),'validRps':round(sum(r['valid'] for r in rows)/elapsed,2),'statuses':{str(s):sum(r['http']==s for r in rows) for s in sorted(set(r['http'] for r in rows))},'validationOrTransportErrors':sum(bool(r['error']) for r in rows),'p95EndToEndMs':percentile([r['endToEndMs'] for r in rows],.95),'p95QueueMs':percentile([r['queueMs'] for r in rows],.95)}
    RESULT['stages'].append(summary);RAW.flush();save();print(json.dumps(summary,ensure_ascii=False),flush=True)

def serial_samples():
    endpoints=[('/articles/published?size=20',published_data),('/articles/published?current=300&size=20',published_data),('/search?q=deepneedle&size=20',search_data),('/comments/19000001/replies?size=20',lambda d:d['totalRow']==FIX['hotReplies']),('/api/profile/8000001',profile_data)]
    for path,validate in endpoints:
        samples=[]
        for _ in range(3):
            start=time.perf_counter()
            try:
                status,data,h,ms,size=request(path)
                valid=status==200 and data.get('code')==200
                if valid:assert validate(data['data']) is not False
                row={'http':status,'valid':valid,'ms':round(ms,2)}
            except Exception as e:row={'http':0,'valid':False,'ms':round((time.perf_counter()-start)*1000,2),'error':str(e)}
            samples.append(row)
        RESULT.setdefault('serial',[]).append({'path':path,'samples':samples});save()
    print(json.dumps(RESULT['serial'],ensure_ascii=False),flush=True)

def default_limits():
    codes=[]
    for _ in range(35):
        s,d,h,*_=request('/search?q=deepneedle')
        if s==200:search_data(d['data'])
        else:assert s==429 and int(h['retry-after'])>0 and h.get('cache-control')=='no-store'
        codes.append(s)
    assert codes[:30]==[200]*30 and codes[30:]==[429]*5,f'默认账号30次/分钟预算与响应不符:{codes}'
    # 客户端伪造换IP不应重置账号预算。
    for i in range(3):
        s,*_=request('/search?q=deepneedle',extra_headers={'X-Forwarded-For':f'192.0.2.{i+1}'})
        assert s==429,'伪造来源头绕过了已耗尽的账号预算'
    return {'accepted':codes.count(200),'limited':codes.count(429),'retryAfterAndNoStore':True,'spoofedForwardedHeaderCannotBypass':True}

def dependency_timeout():
    ok('/articles/published?size=20')
    # 暂停的是本轮独立16379端口的临时Redis，服务本身保持运行且会自动恢复。
    command=['wsl','-d','Ubuntu-24.04','--exec','redis-cli','-p','16379','CLIENT','PAUSE','4500','ALL']
    pause=subprocess.run(command,capture_output=True,timeout=10)
    assert pause.returncode==0,'无法注入临时Redis暂停'
    status,data,headers,ms,_=request('/articles/published?size=20',timeout=10)
    (OUT/'redis-timeout-response.json').write_text(json.dumps({'status':status,'body':data,'elapsedMs':ms},ensure_ascii=False,indent=2),encoding='utf-8')
    assert status==503, f'依赖不可用应返回503，实际{status}'
    assert 'deepneedle' not in json.dumps(data),'依赖失败时仍返回了受保护的正文'
    assert ms<8000,'依赖失败未及时结束请求'
    for _ in range(8):
        time.sleep(1)
        s,d,*_=request('/articles/published?size=20')
        if s==200:published_data(d['data']);break
    else:raise AssertionError('临时Redis恢复后业务未恢复')
    for _ in range(30):published_data(ok('/articles/published?size=20'))
    return {'failureStatus':status,'failureMs':round(ms,2),'recoveredValidRequests':30}

def media_lifecycle():
    from PIL import Image
    image=Image.new('RGB',(128,64),(31,112,149));buf=io.BytesIO();image.save(buf,format='PNG')
    original=buf.getvalue();boundary='nebula-isolated-multipart-2917'
    payload=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="isolated.png"\r\nContent-Type: image/png\r\n\r\n').encode()+original+f'\r\n--{boundary}--\r\n'.encode()
    conn=http.client.HTTPConnection('127.0.0.1',18080,timeout=10)
    conn.request('POST','/media/images',payload,{'Authorization':'Bearer '+TOKENS[1],'Content-Type':'multipart/form-data; boundary='+boundary})
    res=conn.getresponse();data=json.loads(res.read());assert res.status==200 and data['code']==200
    asset=data['data'];ident=asset['id'];path=asset['path']
    (OUT/'media-created.json').write_text(json.dumps({'id':ident,'path':path}),encoding='utf-8')
    assert path.startswith('upload/article/deep_actor_1/') and ':' not in path
    s,raw,h,*_=request(f'/media/{ident}/file',raw=True);assert s==200
    decoded=Image.open(io.BytesIO(raw));assert decoded.size==(128,64)
    assert decoded.convert('RGB').getpixel((64,32))==(31,112,149)
    assert h.get('x-content-type-options')=='nosniff'
    for route,actor,method in [(f'/media/{ident}/file',2,'GET'),('/'+path,None,'GET'),(f'/media/{ident}',2,'DELETE')]:
        s,d,*_=request(route,actor=actor,method=method)
        assert s in (403,404),f'闲置图片的跨账号或公开访问未拒绝:{method}:{route}:{s}'
    ok(f'/media/{ident}',method='DELETE')
    s,*_=request(f'/media/{ident}/file');assert s==404
    # 相对资源路径映射到配置的独立上传根目录，删除后不得遗留原图。
    assert not (ROOT/'upload'/path.removeprefix('upload/')).exists(),'媒体删除后遗留原图'
    return {'uploadAndPixels':True,'foreignAndPublicDenied':True,'deletedRowAndFileUnavailable':True}

def disconnected_clients():
    # 在请求发出后主动断开，验证服务端最终能释放许可并继续处理后续请求。
    payload=('GET /api/profile/8000001 HTTP/1.1\r\nHost: 127.0.0.1:18080\r\nAuthorization: Bearer '+TOKENS[1]+'\r\nConnection: close\r\n\r\n').encode()
    def abort(_):
        with socket.create_connection(('127.0.0.1',18080),timeout=5) as sock:
            sock.sendall(payload);time.sleep(.15)
    start=time.perf_counter()
    with cf.ThreadPoolExecutor(max_workers=16) as pool:list(pool.map(abort,range(16)))
    time.sleep(3)
    for _ in range(8):
        try:
            data=ok('/articles/published?size=20');published_data(data);break
        except Exception:time.sleep(2)
    else:raise AssertionError('客户端断开后业务没有恢复')
    for _ in range(10):published_data(ok('/articles/published?size=20'))
    return {'disconnectedRequests':16,'recoveryAnd10ReadsSeconds':round(time.perf_counter()-start,3)}

login()
if A.mode=='default':
    check('默认搜索预算及拒绝响应',default_limits)
    check('未认证与参数边界',lambda: all(request(p,actor=None)[0]==401 for p in ['/articles/published','/search?q=deepneedle']))
elif A.mode=='behavior':
    for n,f in [('发布列表总量与20页精确覆盖',publication_pages),('搜索跨页与公开可见性',search_pages),('搜索字面通配符与注入载荷',literal_search),('输入边界与未登录拒绝',boundaries),('草稿归属与公开隔离',private_draft),('个人主页精确归属、计数和隐私',profile_isolation),('阅读标记归属和重试幂等',reading_isolation),('热门评论分页与总数',hot_comments),('备份同版本竞争及账号隔离',workspace_conflict),('点赞同账号并发切换',likes_concurrent)]:check(n,f)
elif A.mode=='load':
    load_stage('warmup',1,5)
    for n in [1,4,8,16,32]:load_stage('closed-loop',n,A.duration)
elif A.mode=='soak':
    for index in range(max(1,A.duration//30)):load_stage(f'soak-{index+1}',4,30)
elif A.mode=='arrival':
    for rate in [2,5]:arrival_stage(rate,A.duration)
elif A.mode=='serial':serial_samples()
elif A.mode=='recovery':
    check('依赖恢复后连续30次有效请求',lambda: [published_data(ok('/articles/published?size=20')) for _ in range(30)])
elif A.mode=='fault':check('Redis超时拒绝访问及依赖恢复',dependency_timeout)
elif A.mode=='media':check('图片上传像素核验、归属、公开隔离和删除',media_lifecycle)
elif A.mode=='disconnect':check('客户端中断后的许可释放和业务恢复',disconnected_clients)
elif A.mode=='profile':check('个人主页精确归属、计数和隐私',profile_isolation)
save();RAW.close()
print('RESULT_PATH='+str(OUT/'result.json'),flush=True)
sys.exit(1 if any(not x['passed'] for x in RESULT['checks']) or any(x['validationOrTransportErrors'] for x in RESULT['stages']) or any(not s['valid'] for row in RESULT.get('serial',[]) for s in row['samples']) else 0)
