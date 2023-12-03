from com.example.yoga import YogaMain
import AngleNodeDef
import toolkit
import numpy as np
from yogamat import get_mat_data
class YogaPose:
    '''
    type: WarriorII, Tree, ReversePlank, Plank ...etc
    '''
    def __init__(self, type):
        self.type = type
        self.tips = ""
        self.roi, self.angle_def, self.jsonfile_path = self.initialize(type)
        self.angle_dict = self.initialAngleDict()
        self.sample_angle_dict = {}#initialAngleDict
        self.imagePath = ""# temporary use to demo, skip it
        self.initialDetect()

    def initialize(self, type):
        roi = {}
        angle_def = None
        jsonfile_path = ""

        if type == 'Tree Style':
            roi = {
                'LEFT_KNEE': False,
                'LEFT_HIP': False,
                'RIGHT_FOOT_INDEX': False,
                'RIGHT_KNEE': False,
                'RIGHT_HIP': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_INDEX': False,
                'RIGHT_INDEX': False,
            }
            angle_def = AngleNodeDef.TREE_ANGLE
            jsonfile_path = f"JsonFile/TreePose/sample.json"
        elif type == 'Warrior2 Style':
            roi = {
                'RIGHT_ANKLE': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False
            }
            angle_def = AngleNodeDef.WARRIOR_II_ANGLE
            jsonfile_path = f"JsonFile/WarriorIIPose/sample.json"

        elif type == 'Reverse Plank':
            roi = {
                'NOSE': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_INDEX': False,
                'RIGHT_INDEX': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'LEFT_KNEE': False,
                'RIGHT_KNEE': False
            }
            angle_def = AngleNodeDef.REVERSE_PLANK_ANGLE
            jsonfile_path = f"JsonFile/ReversePlankPose/sample.json"
        elif type == "Plank":
            roi = {
                'NOSE': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'LEFT_KNEE': False,
                'RIGHT_KNEE': False,
                'LEFT_ANKLE': False,
                'RIGHT_ANKLE': False,
            }
            angle_def = AngleNodeDef.PLANK_ANGLE
            jsonfile_path = f"JsonFile/PlankPose/sample_v3.json"
        elif type == "Child's pose":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'LEFT_ANKLE': False,
                'RIGHT_ANKLE': False,
            }
            angle_def = AngleNodeDef.CHILDS_ANGLE
            jsonfile_path = f"JsonFile/ChildsPose/sample.json"
        elif type == "Downward dog":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'RIGHT_ANKLE':False,
                'LEFT_ANKLE':False,
                'LEFT_HEEL': False,
                'RIGHT_HEEL': False,
            }
            angle_def = AngleNodeDef.DOWNWARDDOG_ANGLE 
            jsonfile_path = f"JsonFile/DownwardDogPose/sample.json"
        elif type == "Low Lunge":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'RIGHT_ANKLE':False,
                'LEFT_ANKLE':False,
            }
            angle_def = AngleNodeDef.LOWLUNGE_ANGLE 
            jsonfile_path = f"JsonFile/LowLungePose/sample.json"
        elif type == "Seated Forward Bend":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'RIGHT_ANKLE':False,
                'LEFT_ANKLE':False,
                'RIGHT_FOOT_INDEX':False,
                'LEFT_FOOT_INDEX':False,
            }
            angle_def = AngleNodeDef.SEATEDFORWARDBEND_ANGLE
            jsonfile_path = f"JsonFile/SeatedForwardBendPose/sample.json"
        elif type == "Bridge pose":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'RIGHT_ANKLE':False,
                'LEFT_ANKLE':False,
                'RIGHT_FOOT_INDEX':False,
                'LEFT_FOOT_INDEX':False,
            }
            angle_def = AngleNodeDef.BRIDGE_ANGLE 
            jsonfile_path = f"JsonFile/BridgePose/sample.json"
        elif type == "Pyramid pose":
            roi = {
                'NOSE': False,
                'LEFT_SHOULDER': False,
                'RIGHT_SHOULDER': False,
                'LEFT_ELBOW': False,
                'RIGHT_ELBOW': False,
                'LEFT_WRIST': False,
                'RIGHT_WRIST': False,
                'LEFT_HIP': False,
                'RIGHT_HIP': False,
                'RIGHT_KNEE': False,
                'LEFT_KNEE': False,
                'RIGHT_ANKLE':False,
                'LEFT_ANKLE':False,
                'RIGHT_FOOT_INDEX':False,
                'LEFT_FOOT_INDEX':False,
                'LEG': False,
            }
            angle_def = AngleNodeDef.PYRAMID_ANGLE 
            jsonfile_path = f"JsonFile/PyramidPose/sample.json"
        return roi, angle_def, jsonfile_path
    def initialAngleDict(self, dict={}):
        index = 0
        for key,_ in self.angle_def.items():
            dict[key] = 0
            index+=1
        return dict

    def initialDetect(self):
        self.sample_angle_dict = toolkit.readSampleJsonFile(self.jsonfile_path)
        if self.sample_angle_dict == None:
            #return self.initialAngleDict()
            self.sample_angle_dict = toolkit.readSampleJsonFile(self.jsonfile_path)
        #return self.sample_angle_dict

    def detect(self, point, rect , center):

        self.tips = ""
        point3d = []

        mat = get_mat_data(rect , center)

        for i in range(point.size()):
            ang = []
            for j in range(3):
                ang.append(point.get(i).get(j))
            point3d.append(ang)



        if(self.type == 'Tree Style'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                #angle = toolkit.computeAngle(point3d.get(value[0]), point3d.get(value[1]), point3d.get(value[2]))
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.treePoseRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d, mat)
        elif(self.type == 'Warrior2 Style'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.warriorIIPoseRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Reverse Plank'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]][:2], point3d[value[1]][:2], point3d[value[2]][:2])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.reversePlankPoseRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Plank'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]][:2], point3d[value[1]][:2], point3d[value[2]][:2])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.plankPoseRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == "Child's pose"):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]][:2], point3d[value[1]][:2], point3d[value[2]][:2])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.ChildsPoseRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Downward dog'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.DownwardDogRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Low Lunge'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.LowLungeRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == "Seated Forward Bend"):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.SeatedForwardBendRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Bridge pose'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.BridgeRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)
        elif(self.type == 'Pyramid pose'):
            for key,value in self.angle_def.items():
                angle = toolkit.computeAngle(point3d[value[0]], point3d[value[1]], point3d[value[2]])
                self.angle_dict[key] = angle
            self.roi, self.tips, self.imagePath = toolkit.PyramidRule(self.roi, self.tips, self.sample_angle_dict, self.angle_dict, point3d)

        return [self.tips, self.imagePath]




if __name__ == "__main__":
    landmark  = [[-0.04195899, -0.45470068, -0.49800783], [-0.03318192, -0.49662837, -0.4922224], [-0.032739207, -0.4973393, -0.49164993], [-0.033388793, -0.497507, -0.49159402], [-0.06492008, -0.49036375, -0.48721924], [-0.06476935, -0.4911882, -0.4887974], [-0.06557713, -0.49282837, -0.48756254], [0.028702015, -0.51032495, -0.38913843], [-0.11468751, -0.49356246, -0.3765228], [-0.012957713, -0.44161838, -0.45839038], [-0.055624127, -0.43527463, -0.4538186], [0.11355136, -0.38072678, -0.2867345], [-0.15001108, -0.3640738, -0.2830636], [0.105660625, -0.38897547, -0.2670021], [-0.16315629, -0.30791038, -0.28597853], [0.028729225, -0.490607, -0.20702372], [-0.1065853, -0.43440008, -0.32455036], [0.012279185, -0.49882388, -0.19516961], [-0.072593324, -0.46145716, -0.32724383], [0.015803024, -0.5043777, -0.18922848], [-0.0710453, -0.4755236, -0.304786], [0.024810823, -0.4902709, -0.19758111], [-0.09388401, -0.44077945, -0.3161253], [0.11446194, 9.667E-4, -0.011351924], [-0.11425122, -0.022955116, 0.014334617], [0.08581746, -0.3953731, -0.06520676], [-0.13826358, -0.432663, -0.019005297], [0.119602785, -0.3428142, 0.25228384], [-0.0027126078, -0.38588876, 0.25816008], [0.11135201, -0.30023316, 0.30899766], [0.0032428917, -0.35206273, 0.33343795], [0.20238611, -0.6161823, 0.10663971], [0.0078034527, -0.5411762, 0.16228935]]
    yoga  = YogaPose("Tree")
    print(yoga.detect(landmark,None))