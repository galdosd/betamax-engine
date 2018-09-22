from collections import namedtuple
import traceback
import json
import copy
from com.github.galdosd.betamax.scripting import ScriptCallback
from com.github.galdosd.betamax.scripting import EventType

def reboot():
    servicer().rebootEverything()

def create_sprite(templateName, spriteName=None):
        if spriteName is None:
            spriteName = templateName
        return servicer().createSprite(templateName, newSpriteName(spriteName))

def create_sprite_backing(templateName, spriteName=None):
    if spriteName is None:
        spriteName = templateName
    return servicer().createSprite(templateName, newSpriteName(spriteName))

def get_sprite(spriteName):
    return servicer().getSpriteByName(newSpriteName(spriteName))

def set_sprite_location(spriteName,x,y):
    get_sprite(spriteName).setLocation(newTextureCoord(x,y))

def sprite_exists(spriteName):
    return servicer().spriteExists(newSpriteName(spriteName))

# aka whenever.begin
def register_begin(func):
    servicer().registerGlobalCallback(EventType.BEGIN, PyScriptCallback(func))

# aka whenever.sprite_create(sprite_name)(func)
def register_sprite_create(sprite_name):
    def inner_decorator(func):
        servicer().registerSpriteCallback(EventType.SPRITE_CREATE, newSpriteName(sprite_name), 0, PyScriptCallback(func))
    return inner_decorator

# aka whenever.sprite_destroy(sprite_name)(func)
def register_sprite_destroy(sprite_name):
    def inner_decorator(func):
        servicer().registerSpriteCallback(EventType.SPRITE_DESTROY, newSpriteName(sprite_name), 0, PyScriptCallback(func))
    return inner_decorator

# aka whenever.sprite_click(sprite_name, momentid)(func)
def register_sprite_click(sprite_name):
    def inner_decorator(func):
        servicer().registerSpriteCallback(EventType.SPRITE_CLICK, newSpriteName(sprite_name), 0, PyScriptCallback(func))
    return inner_decorator

# aka whenever.sprite_moment(sprite_name, momentid)(func)
# momentid can be a named moment but only if the sprite name is same as its template name
# if they difer, you can manually pass in the template name as a third arg, eg:
# whenever.sprite_moment(sprite_name, momentid, template_name)(func)
def register_sprite_moment(sprite_name, momentid, moment_template_name=None):
    if(isinstance(momentid,str)):
        if moment_template_name is None:
            moment_template_name = sprite_name
        momentid = servicer().getNamedMoment(moment_template_name, momentid)
    def inner_decorator(func):
        servicer().registerSpriteCallback(EventType.SPRITE_MOMENT, newSpriteName(sprite_name), momentid, PyScriptCallback(func))
    return inner_decorator

def after_sprite(first, second, secondName=None):
    def callback():
        destroy_sprite(first)
        create_sprite(second, secondName)
    register_sprite_moment(first, -1)(callback)

def destroy_sprite(name):
    servicer().destroySprite(newSpriteName(name))

def destroy_any_sprite(name):
    if sprite_exists(name):
        destroy_sprite(name)
    else:
        log("destroy_sprite_any: no such sprite " + name)

def load_template(name):
    pass

def log(msg):
    servicer().log(msg)

def ensure(condition, msg="Assertion failed"):
    if not condition:
        servicer().fatal(msg)

def set_global_shader(name):
    servicer().setGlobalShader(name)

# sprites.<your sprite name>
#       gets you a sprite same as get_sprite("sprite name") would have

# state.<state var>
#       lets you access state to be managed by the engine
# state(
#     var1 = val,
#     var2 = val, ...
# )
#       lets you set many state vars at once (eg at init time to save typing)

# @whenever.{begin, sprite_moment, sprite_create, sprite_destroy, sprite_click, etc...}(....)
#       easier way of using register_* functions



# WARNING below stuff is not meant to be used directly by user scripts

class spritesaccess():
    def __getattribute__(self, sprite_name):
        return get_sprite(sprite_name)

class magicdict(object):
    def __init__(self):
        pass
    def __getattribute__(self,attrname):
        return state_decode(servicer().getStateVariable(attrname))
    def __setattr__(self,attrname,attrvalue):
        servicer().setStateVariable(attrname, state_encode(attrvalue))
    def __call__(self, *args, **kwargs):
        for key, val in kwargs.iteritems():
            servicer().setStateVariable(key, state_encode(val))

def state_encode(val):
    return json.dumps(val, sort_keys=True)

def state_decode(val):
    return json.loads(val)

# TODO use a magicdict that actually forwards to servicer state, but this will do for now
state = magicdict()
sprites = spritesaccess()

whenever = namedtuple("whenever", "begin sprite_moment sprite_create sprite_destroy sprite_click")(
    begin = register_begin,
    sprite_moment = register_sprite_moment,
    sprite_create = register_sprite_create,
    sprite_destroy = register_sprite_destroy,
    sprite_click = register_sprite_click
)


class PyScriptCallback(ScriptCallback):
    def __init__(self, func):
        self.func = func
        self.trace = "\n".join(traceback.format_list(traceback.extract_stack()[:-2]))
    def invoke(self):
        self.func()
    def toString(self):
        code = self.func.func_code
        return "[%s:%d] def %s(): ...\n    Registered at:\n%s"%(
            code.co_filename, code.co_firstlineno, code.co_name, self.trace )

SCRIPT_SERVICER_HOLDER = [None]
def register_script_servicer(ss):
    SCRIPT_SERVICER_HOLDER[0] = ss

def servicer():
    return SCRIPT_SERVICER_HOLDER[0]

def newSpriteName(string):
    return servicer().newSpriteName(string)

def newTextureCoord(x,y):
    return servicer().newTextureCoord(x,y)
