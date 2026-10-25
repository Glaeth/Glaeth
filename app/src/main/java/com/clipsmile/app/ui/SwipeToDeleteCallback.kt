package com.clipsmile.app.ui

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.clipsmile.app.R

/**
 * Lets the user swipe a clip card left or right to delete it, drawing a red
 * backdrop with a trash icon behind the card as feedback while dragging.
 */
class SwipeToDeleteCallback(
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwiped(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val background = backgroundDrawable(recyclerView)
        val icon = deleteIcon(recyclerView)

        background.setBounds(itemView.left, itemView.top, itemView.right, itemView.bottom)
        background.draw(c)

        val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
        val iconTop = itemView.top + iconMargin
        val iconBottom = iconTop + icon.intrinsicHeight

        if (dX > 0) {
            icon.setBounds(
                itemView.left + iconMargin,
                iconTop,
                itemView.left + iconMargin + icon.intrinsicWidth,
                iconBottom
            )
        } else {
            icon.setBounds(
                itemView.right - iconMargin - icon.intrinsicWidth,
                iconTop,
                itemView.right - iconMargin,
                iconBottom
            )
        }
        icon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun backgroundDrawable(recyclerView: RecyclerView): Drawable =
        ContextCompat.getDrawable(recyclerView.context, R.drawable.bg_swipe_delete)!!

    private fun deleteIcon(recyclerView: RecyclerView): Drawable =
        ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)!!.apply {
            setTint(ContextCompat.getColor(recyclerView.context, R.color.pikachu_yellow))
        }
}
