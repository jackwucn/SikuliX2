function find() {
  what = null
  where = null
  target = null
  if (arguments.length > 0) {
    what = arguments[0]
    if (arguments.length > 1) {
      where = arguments[1]
    }
    target = Do.find(what, where)
  }
  return target
}

$V11 = find("image")