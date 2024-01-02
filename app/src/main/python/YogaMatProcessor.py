from FeetData import FeetData
import toolkit, AngleNodeDef
import cv2
import numpy as np
from PIL import Image

# 將 A 圖片覆蓋到 B 圖片
def overlay_image(background, overlay, position_x, position_y):
    # Convert the images to RGBA mode
    pil_background = Image.fromarray(cv2.cvtColor(background, cv2.COLOR_BGR2RGBA))
    pil_overlay = Image.fromarray(cv2.cvtColor(overlay, cv2.COLOR_BGR2RGBA))

    # Paste the overlay onto the background
    pil_background.paste(pil_overlay, (position_x, position_y), pil_overlay)

    # Convert the result back to BGR mode
    result = cv2.cvtColor(np.array(pil_background), cv2.COLOR_RGBA2BGR)

    return result


# 將圖片等比例縮放
def resize_image(image, target_length):
    # 取得圖片的原始寬高
    height, width = image.shape[:2]

    # 計算縮放比例
    scale = target_length / max(height, width)

    # 計算新的寬高
    new_width = int(width * scale)
    new_height = int(height * scale)

    # 進行縮放
    resized_image = cv2.resize(image, (new_width, new_height))

    return resized_image


# 劃出在攝影機中，瑜珈墊框起來的狀況
def draw_camera_result(image, contours, max_contour, approx, raw_points):
    image_copy = image.copy()

    cv2.drawContours(image_copy, [max_contour], 0, (0, 255, 0), 2)
    cv2.drawContours(image_copy, [approx], -1, (0, 0, 255), 2)

    for contour in contours:
        cv2.drawContours(image_copy, [contour], 0, (255, 0, 0), 2)

    colors = [(0, 0, 255), (255, 255, 0)]
    # Iterate through the points with corresponding colors
    for i, raw_point in enumerate(raw_points):
        # Draw on the original image
        cv2.circle(image_copy, tuple(map(int, raw_point)), 10, colors[i], thickness=cv2.FILLED)

    return image_copy


# 根據 MediaPipe 的 3d 座標，判斷是否有抬腳
def check_raise_feet(point3d):
    left_point = point3d[AngleNodeDef.LEFT_HEEL]
    right_point = point3d[AngleNodeDef.RIGHT_HEEL]

    raise_feet_dict = {
        AngleNodeDef.LEFT_HEEL: True,
        AngleNodeDef.RIGHT_HEEL: True,  # Assuming True as the default value
    }

    raise_feet_interval = 0.1
    print("left_point[1]", left_point[1], "right_point[1]", right_point[1])

    # 如果兩者的 y 座標差距大於 raise_feet_interval，則更新 raise_feet_dict
    if abs(left_point[1] - right_point[1]) > raise_feet_interval:
        # 比較 left_point 和 right_point 的 y 座標，數值比較低者為抬腳
        if left_point[1] < right_point[1]:
            raise_feet_dict[AngleNodeDef.LEFT_HEEL] = False
        else:
            raise_feet_dict[AngleNodeDef.RIGHT_HEEL] = False

    return raise_feet_dict


class YogaMatProcessor:
    def __init__(self, w, h):
        # 瑜珈墊顏色區間
        self.lower_green = np.array([30, 20, 20])
        self.upper_green = np.array([70, 180, 180])
        # 鏡頭的長寬
        self.w = w
        self.h = h
        # 鏡頭的長寬
        self.camera_flat_points = np.array([[1, 1], [0, 1], [0, 0], [1, 0]], dtype=np.float32)
        # 預設瑜珈墊在鏡頭的座標
        default_mat_point = np.array([[0.8 , 0.97 ],
                                      [0.2 , 0.97 ],
                                      [0.25 , 0.76 ],
                                      [0.75 , 0.76 ]], dtype=np.float32)

        print(default_mat_point)
        # 轉換矩陣
        self.transform_matrix = cv2.getPerspectiveTransform(default_mat_point, self.camera_flat_points)

    # 最終取得腳的方法
    def get_feet_data(self, r_point2d, r_point3d, center):

        print("r_point3d", r_point3d)
        print("r_point2d", r_point2d)
        point3d = []
        for i in range(r_point3d.size()):
            ang = []
            for j in range(3):
                ang.append(r_point3d.get(i).get(j))
            point3d.append(ang)


        point2d = []
        for i in range(r_point2d.size()):
            ang = []
            for j in range(2):
                ang.append(r_point2d.get(i).get(j))
            point2d.append(ang)

        feet_dict = self.get_feet_points_on_mat(point2d, point3d)
        print("feet_dict", feet_dict)

        feet_data = FeetData(feet_dict[AngleNodeDef.LEFT_HEEL], feet_dict[AngleNodeDef.RIGHT_HEEL], center)
        feet_data.get_closer_foot_to_center()

        return feet_data.to_dict()


    # 從 MediaPipe 的點中，取得在瑜珈墊中腳的點
    def get_feet_points_on_mat(self, point2d, point3d):
        if not isinstance(point2d, int) and not isinstance(point3d, int):
            feet_points = self.get_feet_points(point2d)
            transform_points = self.transform_point(feet_points)
            raise_feet_dict = check_raise_feet(point3d)
            print("feet_points", feet_points)
            print("feet transform_points", transform_points)

            feet_points_dict = {
                AngleNodeDef.LEFT_HEEL: (transform_points[0][0], transform_points[0][1]),
                AngleNodeDef.RIGHT_HEEL: (transform_points[1][0], transform_points[1][1]),
            }

            # print("raise_feet_dict", raise_feet_dict)
            print("feet_points_dict", feet_points_dict)

#             for key, feet_on_mat in raise_feet_dict.items():
#                 # 如果抬腳，較刪除該點
#                 if not feet_on_mat:
#                     feet_points_dict[key] = (float("inf"), float("inf"))

            return feet_points_dict
        else:
            return {AngleNodeDef.LEFT_HEEL: (float("inf"), float("inf")),
                                    AngleNodeDef.RIGHT_HEEL: (float("inf"), float("inf"))}

    # 偵測場景中的瑜珈墊，製作轉換矩陣
    def generate_transform_matrix(self, image):
        hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
        mask = cv2.inRange(hsv, self.lower_green, self.upper_green)

        contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        max_contour = max(contours, key=cv2.contourArea)
        hull = cv2.convexHull(max_contour)
        approx = cv2.approxPolyDP(hull, 0.05 * cv2.arcLength(hull, True), True)

        original_points = np.array([approx.reshape(-1, 2)], dtype=np.float32)

        if len(original_points[0]) == 4:
            self.transform_matrix = cv2.getPerspectiveTransform(original_points, self.camera_flat_points)

        # 框出來綠色區塊, 根據綠色區塊製作凸集
        return max_contour, approx

    # 將鏡頭中的座標點，轉換到瑜珈墊上面
    def transform_point(self, input_points):
        transformed_points = [cv2.perspectiveTransform(np.array([np.float32([p])]), self.transform_matrix)[0][0] for p
                              in input_points]

        return transformed_points


    # 從 MediaPipe 的點中，取得在鏡頭中腳的點
    def get_feet_points(self, point2d):
        if not isinstance(point2d, int):
            print("feet get_feet_points L: " + str(point2d[AngleNodeDef.LEFT_HEEL]) + "R:" + str(point2d[AngleNodeDef.RIGHT_HEEL]))
            return [[point2d[AngleNodeDef.LEFT_HEEL][0] , point2d[AngleNodeDef.LEFT_HEEL][1] ],
            [point2d[AngleNodeDef.RIGHT_HEEL][0] , point2d[AngleNodeDef.RIGHT_HEEL][1] ]]
        else:
            return []
