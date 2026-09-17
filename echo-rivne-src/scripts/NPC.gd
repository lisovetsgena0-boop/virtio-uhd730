extends KinematicBody
class_name EchoNPC

var center = Vector3()
var radius = 8.0
var phase = 0.0
var speed = 1.3
var alive_time = 0.0

onready var arm_l = get_node_or_null("Visual/ArmL")
onready var arm_r = get_node_or_null("Visual/ArmR")
onready var leg_l = get_node_or_null("Visual/LegL")
onready var leg_r = get_node_or_null("Visual/LegR")
onready var torso = get_node_or_null("Visual/Torso")

func setup(c, r, p):
    center = c
    radius = r
    phase = p

func _physics_process(delta):
    alive_time += delta
    var a = phase + alive_time * speed / max(radius, 1.0)
    var target = center + Vector3(cos(a) * radius, global_transform.origin.y, sin(a) * radius)
    var dir = target - global_transform.origin
    dir.y = 0
    if dir.length() > 0.1:
        dir = dir.normalized()
        move_and_slide(dir * speed, Vector3.UP)
        look_at(global_transform.origin + dir, Vector3.UP)
        rotation.x = 0.0
        rotation.z = 0.0
    _animate(delta, dir.length())

func _animate(delta, move_amount):
    if not torso:
        return
    var t = alive_time * 6.0
    var swing = sin(t) * 0.65 * clamp(move_amount * 1.5, 0.0, 1.0)
    torso.translation.y = 0.95 + abs(sin(t * 0.5)) * 0.05
    if arm_l:
        arm_l.rotation.x = swing
    if arm_r:
        arm_r.rotation.x = -swing
    if leg_l:
        leg_l.rotation.x = -swing
    if leg_r:
        leg_r.rotation.x = swing
