package com.kithlo.noteboox.data.file

import org.json.JSONObject

abstract class Node(val icon: Int) {
    abstract val description: String
    abstract var name: String
    abstract var parent: BranchNode

    val isRoot: Boolean
        get() = this == parent

    abstract fun fromJSON(obj: JSONObject)
    abstract fun toJSON(): JSONObject

    fun moveTo(branch: BranchNode): Boolean {
        if (branch == parent) {
            return false
        }
        if (!branch.addChild(this)) {
            return false
        }
        parent.removeChild(this)
        return true
    }

    fun copyTo(branch: BranchNode): Boolean {
        if (branch == parent) {
            return false
        }
        val cloned = deepCopy()
        return branch.addChild(cloned)
    }

    fun rename(newName: String): Boolean {
        name = newName
        return true
    }

    fun remove() {
        parent.removeChild(this)
    }

    abstract fun deepCopy(): Node

    protected fun loadNode(obj: JSONObject): Node {
        val type = obj.get("type")
        if (type == "branch") {
            val branch = BranchNode()
            branch.fromJSON(obj)
            return branch
        } else if (type == "leaf") {
            val leaf = LeafNode()
            leaf.fromJSON(obj)
            return leaf
        }
        throw IllegalArgumentException("Invalid JSON object")
    }
}