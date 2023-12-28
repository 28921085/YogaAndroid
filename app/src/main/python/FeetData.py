
import numpy as np

class FeetData:
    def __init__(self, left_foot, right_foot, center_of_gravity=None):
        # 將 int64 轉換為 int
        self.left_foot = self.convert_to_int(left_foot)
        self.right_foot = self.convert_to_int(right_foot)
        if not center_of_gravity:
            self.center_of_gravity = [0, 0]
        else:
            self.center_of_gravity = self.convert_to_int(center_of_gravity)

    @staticmethod
    def convert_to_int(value):
        if isinstance(value, np.ndarray) and value.dtype == np.int64:
            return int(value)
        return value

    def get_non_empty_feet_count(self):
        # 回傳非空腳的數量
        non_empty_feet_count = sum(1 for f in (self.left_foot, self.right_foot) if f is not None)
        return non_empty_feet_count

    def get_closer_foot_to_center(self):
        print("feet", type(self.left_foot))
        # 回傳是左腳還是右腳比較靠近重心座標
        right_foot_distance = (
                abs(self.right_foot[0] - self.center_of_gravity[0]) +
                abs(self.right_foot[1] - self.center_of_gravity[1])
                if self.right_foot is not None
                else float("inf")
        )

        left_foot_distance = (
                abs(self.left_foot[0] - self.center_of_gravity[0]) +
                abs(self.left_foot[1] - self.center_of_gravity[1])
                if self.left_foot is not None
                else float("inf")
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

    def __str__(self):
        return f"left_foot = {self.left_foot}, right_foot = {self.right_foot}, center = {self.center_of_gravity}"