package com.example.yoga.Model

import android.content.Context
import com.example.yoga.R

class fileNameGetter {
    //獲取影片檔案
     fun getfile(context: Context, filename: String): Int {
        return when (filename) {
            "Tree Style" -> R.raw.tree_style
            "Warrior2 Style" -> R.raw.warrior2_style
            "Plank" -> R.raw.plank
            "Reverse Plank" -> R.raw.reverse_plank
            "Child's pose" -> R.raw.childspose_teacher
            "Seated Forward Bend" -> R.raw.seatedforwardbend_teacher
            "Low Lunge" -> R.raw.lowlunge_teacher
            "Downward dog" -> R.raw.downwarddog_teacher
            "Pyramid pose" -> R.raw.pyramid_teacher
            "Bridge pose" -> R.raw.bridge_teacher
            "Mountain pose" -> R.raw.mountain_teacher
            "Triangle pose" -> R.raw.triangle_teacher
            "Locust pose" -> R.raw.locust_teacher
            "Cobra pose" -> R.raw.cobra_teacher
            "Half moon pose" -> R.raw.half_moon_teacher
            "Boat pose" -> R.raw.boat_teacher
            "Camel pose" -> R.raw.camel_teacher
            "Pigeon pose" -> R.raw.pigeon_teacher
            "Fish pose" -> R.raw.fish_teacher
            "Chair pose" -> R.raw.chair_teacher
            else -> R.raw.tree_style
        }
    }
    // Function to get image resource based on poseName
    fun getDefaultPic(filename: String?): String {
        return when (filename) {
            "Tree Style" -> "Tree Style/8"
            "Warrior2 Style" -> "Warrior2 Style/8"
            "Plank" -> "Plank/10"
            "Reverse Plank" -> "Reverse Plank/6"
            "Child's pose" -> "Child's pose/5"
            "Seated Forward Bend" -> "Seated Forward Bend/5"
            "Low Lunge" -> "Low Lunge/5"
            "Downward dog" -> "Downward dog/6"
            "Pyramid pose" -> "Pyramid pose/6"
            "Bridge pose" -> "Bridge pose/5"
            "Mountain pose" -> "Mountain pose/1"
            "Triangle pose" -> "Triangle pose/1"
            "Locust pose" -> "Locust pose/1"
            "Cobra pose" -> "Cobra pose/1"
            "Half moon pose" -> "Half moon pose/1"
            "Boat pose" -> "Boat pose/1"
            "Camel pose" -> "Camel pose/1"
            "Pigeon pose" -> "Pigeon pose/1"
            "Fish pose" -> "Fish pose/1"
            "Chair pose" -> "Chair pose/1"
            else -> "Tree Style/8"
        }
    }
    // Function to get image resource based on poseName
    fun getPoseFolder(filename: String?): String {
        return when (filename) {
            "Tree Style" -> "Tree Style"
            "Warrior2 Style" -> "Warrior2 Style"
            "Plank" -> "Plank"
            "Reverse Plank" -> "Reverse Plank"
            "Child's pose" -> "Child's pose"
            "Seated Forward Bend" -> "Seated Forward Bend"
            "Low Lunge" -> "Low Lunge"
            "Downward dog" -> "Downward dog"
            "Pyramid pose" -> "Pyramid pose"
            "Bridge pose" -> "Bridge pose"
            "Mountain pose" -> "Mountain pose"
            "Triangle pose" -> "Triangle pose"
            "Locust pose" -> "Locust pose"
            "Cobra pose" -> "Cobra pose"
            "Half moon pose" -> "Half moon pose"
            "Boat pose" -> "Boat pose"
            "Camel pose" -> "Camel pose"
            "Pigeon pose" -> "Pigeon pose"
            "Fish pose" -> "Fish pose"
            "Chair pose" -> "Chair pose"
            else -> "Tree Style"
        }
    }
}