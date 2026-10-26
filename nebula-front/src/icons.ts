import { addIcons } from 'ionicons'
import { defineCustomElement } from 'ionicons/components/ion-icon'
import {
  chatbubbleEllipsesOutline,
  chatbubbleOutline,
  documentTextOutline,
  eyeOutline,
  folderOutline,
  heartOutline,
  homeOutline,
  lockClosedOutline,
  logOutOutline,
  peopleOutline,
  personCircleOutline,
  pricetagOutline,
  shieldOutline,
  trashOutline,
} from 'ionicons/icons'

// 所有使用中的图标随应用构建，保持原图形且不再从外部CDN执行脚本或获取SVG。
addIcons({
  'chatbubble-ellipses-outline': chatbubbleEllipsesOutline,
  'chatbubble-outline': chatbubbleOutline,
  'document-text-outline': documentTextOutline,
  'eye-outline': eyeOutline,
  'folder-outline': folderOutline,
  'heart-outline': heartOutline,
  'home-outline': homeOutline,
  'lock-closed-outline': lockClosedOutline,
  'log-out-outline': logOutOutline,
  'people-outline': peopleOutline,
  'person-circle-outline': personCircleOutline,
  'pricetag-outline': pricetagOutline,
  'shield-outline': shieldOutline,
  'trash-outline': trashOutline,
})
defineCustomElement()
