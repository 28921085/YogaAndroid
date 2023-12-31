
import numpy as np

# !!!!!!! 請注意以下的 TODO
class FeetData:
    def __init__(self, left_foot, right_foot, center_of_gravity=None):
        # 將 int64 轉換為 int
        # TODO : 由於目前 輸入的 MediaPipe 座標左右相反， 先階段決定先維持現狀，因此在 FeetData 中將左右腳設定相反
        self.left_foot = self.convert_to_int(right_foot)
        self.right_foot = self.convert_to_int(left_foot)
        print("center in ", type(center_of_gravity))
        if center_of_gravity is None or (isinstance(center_of_gravity, np.ndarray) and np.size(center_of_gravity) == 0):
            self.center_of_gravity = [0, 0]
        else:
            print("center in ", center_of_gravity)
            self.center_of_gravity = center_of_gravity
            print("center af ", self.center_of_gravity)

    def build(self, data):
        self.left_foot = data["left_foot"]
        self.right_foot = data["right_foot"]

    @staticmethod
    def convert_to_int(value):
        if isinstance(value, np.ndarray) and value.dtype == np.int64:
            return int(value)
        return value

    def get_non_empty_feet_count(self):
        # 回傳非空腳的數量
        non_empty_feet_count = sum(1 for f in (self.left_foot, self.right_foot) if f is not float("inf"))
        return non_empty_feet_count

    def get_closer_foot_to_center(self):
        print("center", self.center_of_gravity)
        # 回傳是左腳還是右腳比較靠近重心座標
        right_foot_distance = (
                abs(self.right_foot[0] - self.center_of_gravity[0]) +
                abs(self.right_foot[1] - self.center_of_gravity[1])
        )

        left_foot_distance = (
                abs(self.left_foot[0] - self.center_of_gravity[0]) +
                abs(self.left_foot[1] - self.center_of_gravity[1])
        )

        if right_foot_distance < left_foot_distance:
            return "RIGHT_HEEL"
        elif left_foot_distance < right_foot_distance:
            return "LEFT_HEEL"
        else:
            return "Equal"

    def to_dict(self):
        # 將 FeetData 物件轉換為字典
        return {"left_foot": self.left_foot, "right_foot": self.right_foot, "center_of_gravity": self.center_of_gravity}


    @classmethod
    def from_dict(cls, data):
        # 從字典反序列化成 FeetData 物件
        return cls(data["left_foot"], data["right_foot"], data.get("center_of_gravity"))

    def get_left_foot_x(self):
        # 取得 left_foot 的 x 座標
        return self.left_foot[0] if self.left_foot is not None else - 999999

    def get_left_foot_y(self):
        # 取得 left_foot 的 y 座標
        return self.left_foot[1] if self.left_foot is not None else - 999999

    def get_right_foot_x(self):
        # 取得 right_foot 的 x 座標
        return self.right_foot[0] if self.right_foot is not None else - 999999

    def get_right_foot_y(self):
        # 取得 right_foot 的 y 座標
        return self.right_foot[1] if self.right_foot is not None else - 999999

    def __str__(self):
        return f"left_foot = {self.left_foot}, right_foot = {self.right_foot}, center = {self.center_of_gravity}"