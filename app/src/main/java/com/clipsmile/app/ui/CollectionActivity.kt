package com.clipsmile.app.ui

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clipsmile.app.R
import com.clipsmile.app.data.Den
import com.clipsmile.app.databinding.ActivityCollectionBinding
import com.clipsmile.app.databinding.ItemCollectionDogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionBinding
    private lateinit var den: Den

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        den = Den(this)
        binding.backButton.setOnClickListener { finish() }
        showDogs()
    }

    private fun showDogs() {
        val openCount = DogRoster.all.count { den.owns(it.stableId()) }
        binding.progressLabel.text = getString(R.string.collection_progress, openCount, DogRoster.all.size)
        binding.collectionProgress.max = DogRoster.all.size
        binding.collectionProgress.progress = openCount
        binding.dogList.removeAllViews()
        val selected = den.activeId()
        val density = resources.displayMetrics.density
        var line: LinearLayout? = null
        for ((index, dog) in DogRoster.all.withIndex()) {
            if (index % 2 == 0) {
                line = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = (10 * density).toInt() }
                }
                binding.dogList.addView(line)
            }
            val row = ItemCollectionDogBinding.inflate(layoutInflater, line, false)
            val params = row.root.layoutParams as LinearLayout.LayoutParams
            val gap = (5 * density).toInt()
            if (index % 2 == 0) params.marginEnd = gap else params.marginStart = gap
            row.root.layoutParams = params
            line!!.addView(row.root)
            val open = den.owns(dog.stableId())
            val atHome = open && dog.stableId() == selected
            val look = FrameLooks.forLevel(if (open) den.level(dog.stableId()) else 1)
            row.portrait.setImageBitmap(faceOf(dog))
            row.portrait.alpha = if (open) 1f else 0.35f
            row.foodIcon.setImageResource(dog.foodRes)
            row.foodIcon.alpha = if (open) 1f else 0.4f
            row.foodIcon.contentDescription = getString(dog.foodNameRes)
            row.ring.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (open) look.glow else 0x22A89880)
                setStroke((if (open) look.widthDp else 3f).dp(), if (open) look.ring else 0xFFC4B6A6.toInt())
            }
            val species = dog.displayName(this)
            val name = if (open) den.displayName(dog.stableId(), species) else species
            row.name.text = name
            row.breed.text = dog.displayTitle(this)
            row.levelChip.text = if (open) {
                getString(R.string.collection_level_chip, den.level(dog.stableId()))
            } else {
                DogRoster.priceLabel(dog)
            }
            row.foodChip.setText(dog.foodNameRes)
            row.frameChip.setText(look.nameRes)
            row.frameChip.setTextColor(if (open) look.ring else 0xFF8A6844.toInt())
            row.homeBadge.visibility = if (atHome) View.VISIBLE else View.GONE
            row.date.text = if (!open) {
                getString(R.string.catalog_locked)
            } else {
                getString(R.string.xp_line, den.into(dog.stableId()), den.need(dog.stableId()))
            }
            row.card.setBackgroundResource(
                if (atHome) R.drawable.bg_collection_card_on else R.drawable.bg_collection_card
            )
            row.card.elevation = (if (atHome) 6f else 1.5f) * density
            row.card.alpha = if (open) 1f else 0.78f
            if (!open) {
                row.card.setOnClickListener {
                    Toast.makeText(this, R.string.catalog_locked, Toast.LENGTH_SHORT).show()
                }
            } else {
                row.card.setOnClickListener {
                    den.setActive(dog.stableId())
                    Toast.makeText(this, getString(R.string.collection_selected, name), Toast.LENGTH_SHORT).show()
                    showDogs()
                }
                row.name.setOnClickListener { rename(dog.stableId(), dog.displayName(this)) }
            }
        }
        if (DogRoster.all.size % 2 == 1) {
            val spacer = View(this)
            spacer.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
            line?.addView(spacer)
        }
    }

    private fun faceOf(dog: DogCompanion) = try {
        PixelArt.load(this, dog.portraitAsset.ifBlank { "faces/${dog.stableId()}.png" })
    } catch (_: Exception) {
        BitmapFactory.decodeResource(resources, dog.portraitRes)
    }

    private fun rename(id: String, fallback: String) {
        val input = EditText(this).apply {
            setText(den.displayName(id, fallback))
            setSelection(text.length)
            setSingleLine()
            setTextColor(Color.parseColor("#3B2918"))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_rename_pet)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save_name) { _, _ ->
                den.setName(id, input.text?.toString().orEmpty())
                showDogs()
            }
            .show()
    }

    private fun Float.dp(): Int = (this * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
