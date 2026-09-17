extends Reference
class_name SaveSystem

const PATH = "user://echo_rivne_save.json"

static func save(data):
    var f = File.new()
    if f.open(PATH, File.WRITE) != OK: return false
    f.store_string(to_json(data))
    f.close()
    return true

static func load():
    var f = File.new()
    if not f.file_exists(PATH): return null
    if f.open(PATH, File.READ) != OK: return null
    var txt = f.get_as_text()
    f.close()
    return parse_json(txt)
