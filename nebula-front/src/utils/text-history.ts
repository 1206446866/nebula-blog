export interface TextSnapshot {
  content: string
  selectionStart: number
  selectionEnd: number
  selectionDirection?: 'forward' | 'backward' | 'none'
}

/** 正文输入和程序插入共用的历史；只合并连续输入，不跨工具操作、选区变化或重做分支。 */
export function createTextHistory(limit = 100, characterLimit = 2_000_000) {
  let states: TextSnapshot[] = []
  let index = -1
  let lastGroup: string | undefined
  let lastTime = 0
  const copy = (state: TextSnapshot): TextSnapshot => ({ ...state })
  const sameSelection = (a: TextSnapshot, b: TextSnapshot) =>
    a.selectionStart === b.selectionStart && a.selectionEnd === b.selectionEnd

  function boundary() {
    lastGroup = undefined
  }
  function reset(state: TextSnapshot) {
    states = [copy(state)]
    index = 0
    boundary()
  }
  function selection(state: TextSnapshot) {
    const current = states[index]
    if (current?.content !== state.content) return
    if (!sameSelection(current, state)) boundary()
    states[index] = copy(state)
  }
  function record(before: TextSnapshot, after: TextSnapshot, group?: string, time = Date.now()) {
    if (states[index]?.content !== before.content) reset(before)
    if (before.content === after.content) {
      selection(after)
      boundary()
      return
    }
    const merge =
      group &&
      group === lastGroup &&
      time - lastTime < 750 &&
      index > 0 &&
      index === states.length - 1 &&
      sameSelection(states[index]!, before) &&
      before.selectionStart === before.selectionEnd &&
      after.selectionStart === after.selectionEnd
    states[index] = copy(before)
    states.splice(index + 1)
    if (merge) states[index] = copy(after)
    else {
      states.push(copy(after))
      index++
    }
    lastGroup = group
    lastTime = time
    let characters = states.reduce((sum, state) => sum + state.content.length, 0)
    while (states.length > 2 && (states.length > limit + 1 || characters > characterLimit)) {
      characters -= states.shift()!.content.length
      index--
    }
  }
  function move(delta: number, current: TextSnapshot) {
    boundary()
    if (states[index]?.content !== current.content) {
      reset(current)
      return null
    }
    selection(current)
    const next = index + delta
    if (next < 0 || next >= states.length) return null
    index = next
    return copy(states[index]!)
  }
  return {
    reset,
    selection,
    boundary,
    record,
    undo: (current: TextSnapshot) => move(-1, current),
    redo: (current: TextSnapshot) => move(1, current),
    get canUndo() {
      return index > 0
    },
    get canRedo() {
      return index >= 0 && index < states.length - 1
    },
  }
}
