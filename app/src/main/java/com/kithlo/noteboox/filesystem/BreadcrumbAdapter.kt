package com.kithlo.noteboox.filesystem

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kithlo.noteboox.data.file.BranchNode
import com.kithlo.noteboox.databinding.ComponentBreadcrumbItemBinding

class BreadcrumbAdapter(
    rootNode: BranchNode,
    private val onRootChange: (node: BranchNode) -> Unit
) : RecyclerView.Adapter<BreadcrumbAdapter.ViewHolder>() {
    private var nodeList = getNodeList(rootNode)

    @SuppressLint("NotifyDataSetChanged")
    fun setRoot(rootNode: BranchNode) {
        nodeList = getNodeList(rootNode)
        notifyDataSetChanged()
    }

    private fun getNodeList(rootNode: BranchNode): List<BranchNode> {
        var node = rootNode
        val list = mutableListOf<BranchNode>()
        while (!node.isRoot) {
            list.add(node)
            node = node.parent
        }
        list.add(node)
        list.reverse()
        return list.toList()
    }

    class ViewHolder(val binding: ComponentBreadcrumbItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ComponentBreadcrumbItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val node = nodeList.getOrNull(position) ?: return
        holder.binding.text = if (node.isRoot) "Notes" else node.name
        holder.binding.first = node.isRoot
        holder.binding.onClick = {
            onRootChange(node)
        }
    }

    override fun getItemCount(): Int {
        return nodeList.size
    }
}