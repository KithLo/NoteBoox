package com.kithlo.noteboox.data.file

import com.kithlo.noteboox.R
import com.kithlo.noteboox.data.note.*
import org.json.JSONObject
import java.util.*

data class LeafNode(
    override var name: String = "",
) : Node(R.drawable.ic_text_snippet) {
    override var parent: BranchNode = BranchNode()

    var uuid: UUID = UUID.randomUUID()
        private set
    var date: String = ""
    var width: Int = DEFAULT_WIDTH
    var height: Int = DEFAULT_HEIGHT
    var pages: Int = 1
    var paper: Paper = NonePaper

    override val description: String
        get() = "$pages page${if (pages == 1) "" else "s"}"

    override fun deepCopy(): LeafNode {
        return copy()
    }

    override fun fromJSON(obj: JSONObject) {
        name = obj.getString("name")
        uuid = UUID.fromString(obj.getString("uuid"))
        date = obj.getString("date")
        width = obj.getInt("width")
        height = obj.getInt("height")
        pages = obj.getInt("pages")

        val paperObj = obj.getJSONObject("paper")

        val paperType = PaperType.valueOf(paperObj.getString("type"))
        paper = when (paperType) {
            PaperType.LINE -> LinePaper(rowHeight = paperObj.getInt("rowHeight"))
            PaperType.GRID -> GridPaper(
                rowHeight = paperObj.getInt("rowHeight"),
                columnWidth = paperObj.getInt("columnWidth")
            )
            else -> NonePaper
        }
    }

    override fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "leaf")
        obj.put("name", name)
        obj.put("uuid", uuid.toString())
        obj.put("date", date)
        obj.put("width", width)
        obj.put("height", height)
        obj.put("pages", pages)

        val paperObj = JSONObject()
        paperObj.put("type", paper.type)

        when (val paper = paper) {
            is LinePaper -> {
                paperObj.put("rowHeight", paper.rowHeight)
            }
            is GridPaper -> {
                paperObj.put("rowHeight", paper.rowHeight)
                paperObj.put("columnWidth", paper.columnWidth)
            }
        }

        obj.put("paper", paperObj)

        return obj
    }

    companion object {
        const val DEFAULT_WIDTH = 1404
        const val DEFAULT_HEIGHT = 1404
    }
}
