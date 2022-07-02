package com.kithlo.noteboox.data.file

import org.json.JSONObject

data class FileTree(val version: Int = CURRENT_VERSION, val root: BranchNode = BranchNode()) {

    fun load(obj: JSONObject) {
        val rootObj = obj.getJSONObject("root")
        root.fromJSON(rootObj)
    }

    fun save(): JSONObject {
        val obj = JSONObject()
        obj.put("version", version)
        obj.put("root", root.toJSON())
        return obj
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}