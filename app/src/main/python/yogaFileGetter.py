import toolkit
import AngleNodeDef
import AngleRegion

yogaFileDict = {
    "Tree Style": {
        "roi": AngleRegion.TREE,
        "angle_def": AngleNodeDef.TREE_ANGLE,
        "default_image_path" : "image/Tree Style/8.jpg"
    },
    "Warrior2 Style": {
        "roi": AngleRegion.WARRIOR_II,
        "angle_def": AngleNodeDef.WARRIOR_II_ANGLE,
        "default_image_path" : "image/Warrior2 Style/8.jpg"
    },
    "Plank": {
        "roi": AngleRegion.PLANK,
        "angle_def": AngleNodeDef.PLANK_ANGLE,
        "default_image_path" : "image/Plank/10.jpg"
    },
    "Reverse Plank": {
        "roi": AngleRegion.REVERSE_PLANK,
        "angle_def": AngleNodeDef.REVERSE_PLANK_ANGLE,
        "default_image_path" : "image/Reverse Plank/6.jpg"
    },
    "Child's pose": {
        "roi": AngleRegion.CHILDS,
        "angle_def": AngleNodeDef.CHILDS_ANGLE,
        "default_image_path" : "image/Child's pose/5.jpg"
    },
    "Downward dog": {
        "roi": AngleRegion.DOWNWARDDOG,
        "angle_def": AngleNodeDef.DOWNWARDDOG_ANGLE,
        "default_image_path" : "image/Downward dog/6.jpg"
    },
    "Low Lunge": {
        "roi": AngleRegion.LOWLUNGE,
        "angle_def": AngleNodeDef.LOWLUNGE_ANGLE,
        "default_image_path" : "image/Low Lunge/5.jpg"
    },
    "Seated Forward Bend": {
        "roi": AngleRegion.SEATEDFORWARDBEND,
        "angle_def": AngleNodeDef.SEATEDFORWARDBEND_ANGLE,
        "default_image_path" : "image/Seated Forward Bend/5.jpg"
    },
    "Bridge pose": {
        "roi": AngleRegion.BRIDGE,
        "angle_def": AngleNodeDef.BRIDGE_ANGLE,
        "default_image_path" : "image/Bridge pose/5.jpg"
    },
    "Pyramid pose": {
        "roi": AngleRegion.PYRAMID,
        "angle_def": AngleNodeDef.PYRAMID_ANGLE,
        "default_image_path" : "image/Pyramid pose/6.jpg"
    },
    "Mountain pose":{
        "roi": AngleRegion.MOUNTAIN,
        "angle_def": AngleNodeDef.MOUNTAIN_ANGLE,
        "default_image_path" : "image/Mountain pose/1.jpg"
    },
    "Triangle pose":{
        "roi": AngleRegion.TRIANGLE,
        "angle_def": AngleNodeDef.TRIANGLE_ANGLE,
        "default_image_path" : "image/Triangle pose/1.jpg"
    },

   "Locust Pose": {
       "roi": AngleRegion.LOCUST,
       "angle_def": AngleNodeDef.LOCUST_ANGLE,
       "default_image_path" : "image/Locust Pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Cobra pose": {
      "roi": AngleRegion.COBRA,
      "angle_def": AngleNodeDef.COBRA_ANGLE,
      "default_image_path" : "image/Cobra pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Half moon pose": {
      "roi": AngleRegion.HALF_MOON,
      "angle_def": AngleNodeDef.HALF_MOON_ANGLE,
      "default_image_path" : "image/Half moon pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Boat pose": {
      "roi": AngleRegion.BOAT,
      "angle_def": AngleNodeDef.BOAT_ANGLE,
      "default_image_path" : "image/Boat pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Camel pose": {
      "roi": AngleRegion.CAMEL,
      "angle_def": AngleNodeDef.CAMEL_ANGLE,
      "default_image_path" : "image/Camel pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Pigeon pose": {
      "roi": AngleRegion.PIGEON,
      "angle_def": AngleNodeDef.PIGEON_ANGLE,
      "default_image_path" : "image/Pigeon pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Fish pose": {
      "roi": AngleRegion.FISH,
      "angle_def": AngleNodeDef.FISH_ANGLE,
      "default_image_path" : "image/Fish pose/1.jpg" #預設瑜珈動作參考圖片
   },

   "Chair pose": {
      "roi": AngleRegion.CHAIR,
      "angle_def": AngleNodeDef.CHAIR_ANGLE,
      "default_image_path" : "image/Chair pose/1.jpg" #預設瑜珈動作參考圖片
   }
}




def get_pose_info(pose):
    if pose in yogaFileDict:
        return yogaFileDict[pose]
    else:
        print("在 Dict 中找不到 ",pose)
        return None

def get_angle_def(pose):
    pose_info = get_pose_info(pose)

    if pose_info is not None:
        return pose_info["angle_def"]
    return None

def get_roi(pose):
    pose_info = get_pose_info(pose)

    if pose_info is not None:
        return pose_info["roi"]
    return {}

def get_sample_angle_dict(pose):
    path = "JsonFile/" + pose + "/sample.json"
    dict = toolkit.readSampleJsonFile(path)
    return dict

def get_sample_score_angle_dict(pose):
    path = "JsonFile/" + pose + "/sample_score.json"
    dict = toolkit.readSampleJsonFile(path)
    return dict

def get_std_angle_dict(pose):
    path = "JsonFile/" + pose + "/std_angle.json"
    dict = toolkit.readSampleJsonFile(path)
    return dict

def get_weight_angle_dict(pose):
    path = "JsonFile/" + pose + "/weight.json"
    dict = toolkit.readSampleJsonFile(path)
    return dict


def get_image_path(pose):
    pose_info = get_pose_info(pose)
    return pose_info["default_image_path"]




