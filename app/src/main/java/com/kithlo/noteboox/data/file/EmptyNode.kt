package com.kithlo.noteboox.data.file

import org.json.JSONObject

object EmptyNode : Node(0) {
    override var name: String = ""
    override val description: String = ""
    override var parent: BranchNode = BranchNode()

    override fun deepCopy(): EmptyNode {
        return this
    }

    override fun fromJSON(obj: JSONObject) {
        throw IllegalArgumentException("Attempt to construct an empty entry.")
    }

    override fun toJSON(): JSONObject {
        throw IllegalArgumentException("Attempt to stringify an empty entry.")
    }
}
