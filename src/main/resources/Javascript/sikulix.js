$LOG = null;
function logTrace() {
  if ($LOG === null) {
    $LOG = SX.getSXLog("SX.JAVASCRIPTRUNNER");
    $LOG.on(SX.TRACE);
  }
  if (arguments.length > 1) {
    $LOG.trace(arguments[0], arguments[1]);
  } else if (arguments.length > 0) {
    $LOG.trace(arguments[0]);
  }
}

function find() {return Do.find(arguments)}
function findAll() {return Do.findAll(arguments)} 
function wait() {return Do.wait(arguments)} 
function waitVanish() {return Do.waitVanish(arguments)}


