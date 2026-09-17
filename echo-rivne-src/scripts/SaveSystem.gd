extends Reference
class_name SaveSystem

const PATH = "user://echo_rivne_save.json"
const SETTINGS_PATH = "user://echo_rivne_settings.json"

static func save(data):
    var f = File.new()
    if f.open(PATH, File.WRITE) != OK:
        return false
    f.store_string(to_json(data))
    f.close()
    return true

static func load():
    var f = File.new()
    if not f.file_exists(PATH):
        return null
    if f.open(PATH, File.READ) != OK:
        return null
    var txt = f.get_as_text()
    f.close()
    return parse_json(txt)

static func save_settings(data):
    var f = File.new()
    if f.open(SETTINGS_PATH, File.WRITE) != OK:
        return false
    f.store_string(to_json(data))
    f.close()
    return true

static func load_settings():
    var f = File.new()
    if not f.file_exists(SETTINGS_PATH):
        return null
    if f.open(SETTINGS_PATH, File.READ) != OK:
        return null
    var txt = f.get_as_text()
    f.close()
    return parse_json(txt)
