package com.firstresources.learningactivity.ui.tugas

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firstresources.learningactivity.R
import com.firstresources.learningactivity.api.models.TaskItem
import com.firstresources.learningactivity.databinding.ItemFileAttachmentBinding
import com.firstresources.learningactivity.databinding.ItemTaskBinding

data class SelectedTaskFile(
    val uri: Uri,
    val name: String,
    val sizeText: String,
    val isPhoto: Boolean
)

class TasksAdapter(
    private var tasks: List<TaskItem>,
    private val onCameraClick: (TaskItem) -> Unit,
    private val onPickFileClick: (TaskItem) -> Unit,
    private val onSubmitTaskClick: (TaskItem, List<Uri>) -> Unit
) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    private var expandedTaskId: Long? = null
    private val selectedFilesMap = mutableMapOf<Long, MutableList<SelectedTaskFile>>()

    class ViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    fun updateList(newList: List<TaskItem>) {
        tasks = newList
        notifyDataSetChanged()
    }

    fun addFiles(taskId: Long, newFiles: List<SelectedTaskFile>) {
        val list = selectedFilesMap.getOrPut(taskId) { mutableListOf() }
        list.addAll(newFiles)
        notifyDataSetChanged()
    }

    fun removeFile(taskId: Long, index: Int) {
        val list = selectedFilesMap[taskId]
        if (list != null && index in list.indices) {
            list.removeAt(index)
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.tvTaskTitle.text = task.title
        holder.binding.tvDeadline.text = "Tenggat: " + (task.deadline ?: "-")

        val isCompleted = task.status.equals("completed", ignoreCase = true) || task.status.equals("submitted", ignoreCase = true)
        if (isCompleted) {
            holder.binding.tvTaskStatus.text = "Selesai"
            holder.binding.tvTaskStatus.setTextColor(Color.parseColor("#047857"))
            holder.binding.tvTaskStatus.setBackgroundResource(R.drawable.bg_badge_hadir)
            holder.binding.btnOpenUpload.visibility = View.GONE
        } else {
            holder.binding.tvTaskStatus.text = "Belum Dikerjakan"
            holder.binding.tvTaskStatus.setTextColor(Color.parseColor("#EF4444"))
            holder.binding.tvTaskStatus.setBackgroundResource(R.drawable.bg_badge_pending)
            holder.binding.btnOpenUpload.visibility = View.VISIBLE
        }

        val isExpanded = expandedTaskId == task.id
        holder.binding.layoutUploadSection.visibility = if (isExpanded) View.VISIBLE else View.GONE

        val files = selectedFilesMap[task.id] ?: emptyList()
        if (files.isEmpty()) {
            holder.binding.layoutEmptyUploadBox.visibility = View.VISIBLE
            holder.binding.layoutSelectedFilesList.visibility = View.GONE
            holder.binding.btnSubmitTask.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            holder.binding.btnSubmitTask.text = "Kirim Tugas"
            holder.binding.btnSubmitTask.isEnabled = false
        } else {
            holder.binding.layoutEmptyUploadBox.visibility = View.GONE
            holder.binding.layoutSelectedFilesList.visibility = View.VISIBLE
            holder.binding.tvFilesCountHeader.text = "File Terpilih (" + files.size + ")"

            // Populate file list container
            val container = holder.binding.containerFilesList
            container.removeAllViews()
            val inflater = LayoutInflater.from(holder.itemView.context)

            files.forEachIndexed { index, fileItem ->
                val fileBinding = ItemFileAttachmentBinding.inflate(inflater, container, false)
                fileBinding.tvFileName.text = fileItem.name
                fileBinding.tvFileSize.text = fileItem.sizeText

                if (fileItem.isPhoto) {
                    fileBinding.ivFileThumb.setImageURI(fileItem.uri)
                } else {
                    fileBinding.ivFileThumb.setImageResource(R.drawable.ic_assignment)
                }

                fileBinding.btnDeleteFile.setOnClickListener {
                    removeFile(task.id, index)
                }

                container.addView(fileBinding.root)
            }

            holder.binding.btnSubmitTask.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D3829"))
            holder.binding.btnSubmitTask.text = "Kirim Tugas (" + files.size + ")"
            holder.binding.btnSubmitTask.isEnabled = true
        }

        holder.binding.btnOpenUpload.setOnClickListener {
            expandedTaskId = if (isExpanded) null else task.id
            notifyDataSetChanged()
        }

        holder.binding.btnCancelUpload.setOnClickListener {
            expandedTaskId = null
            selectedFilesMap.remove(task.id)
            notifyDataSetChanged()
        }

        // Camera actions
        holder.binding.btnCamera.setOnClickListener { onCameraClick(task) }
        holder.binding.btnAddMoreCamera.setOnClickListener { onCameraClick(task) }

        // Browse files actions
        holder.binding.btnBrowseFiles.setOnClickListener { onPickFileClick(task) }
        holder.binding.btnAddMoreFiles.setOnClickListener { onPickFileClick(task) }

        holder.binding.btnSubmitTask.setOnClickListener {
            val list = selectedFilesMap[task.id]
            if (!list.isNullOrEmpty()) {
                onSubmitTaskClick(task, list.map { it.uri })
            }
        }
    }

    override fun getItemCount(): Int = tasks.size
}
