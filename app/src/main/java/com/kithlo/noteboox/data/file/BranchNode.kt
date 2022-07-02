package com.kithlo.noteboox.data.file

import com.kithlo.noteboox.R
import org.json.JSONArray
import org.json.JSONObject

data class BranchNode(
    override var name: String = "",
) : Node(R.drawable.ic_folder) {
    override var parent: BranchNode = this
    private val _children: MutableList<Node> = mutableListOf()
    val children: List<Node>
        get() = _children

    override val description: String
        get() = "${_children.size} item${if (_children.size == 1) "" else "s"}"

    private fun hasChild(name: String): Boolean {
        for (child in _children) {
            if (child.name == name) {
                return true
            }
        }
        return false
    }

    override fun deepCopy(): BranchNode {
        val node = BranchNode(name)
        node.parent = parent
        _children.forEach { oldNode ->
            val newNode = oldNode.deepCopy()
            newNode.parent = node
            node._children.add(newNode)
        }
        return node
    }

    fun addChild(node: Node): Boolean {
        val name = node.name
        if (!hasChild(name)) {
            _children.add(node)
            node.parent = this
            return true
        }
        return false
    }

    fun removeChild(node: Node): Boolean {
        return _children.remove(node)
    }

    override fun fromJSON(obj: JSONObject) {
        name = obj.optString("name", "")
        val childrenArray = obj.optJSONArray("children")
        if (childrenArray != null) {
            for (index in 0 until childrenArray.length()) {
                val childObj = childrenArray.getJSONObject(index)
                val child = loadNode(childObj)
                child.parent = this
                _children.add(child)
            }
        }
    }

    override fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("type", "branch")
        if (name.isNotEmpty()) {
            obj.put("name", name)
        }
        if (_children.isNotEmpty()) {
            val childrenArray = JSONArray()
            for (child in _children) {
                childrenArray.put(child.toJSON())
            }
            obj.put("children", childrenArray)
        }
        return obj
    }
}
