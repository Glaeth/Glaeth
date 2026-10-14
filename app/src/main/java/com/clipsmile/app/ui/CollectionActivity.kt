package com.clipsmile.app.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clipsmile.app.R
import com.clipsmile.app.data.PetProgress
import com.clipsmile.app.databinding.ActivityCollectionBinding
import com.clipsmile.app.databinding.ItemCollectionDogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCollectionBinding
    private lateinit var petProgress: PetProgress

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        petProgress = PetProgress(this)
        val level = petProgress.level()
        petProgress.stampUnlocks(level, DogRoster.all.map { it.minLevel })
        showDogs(level)
    }

    private fun showDogs(level: Int) {
        binding.dogList.removeAllViews()
        val selected = petProgress.selectedLevel()
        for (dog in DogRoster.all) {
            val row = ItemCollectionDogBinding.inflate(layoutInflater, binding.dogList, true)
            val open = level >= dog.minLevel
            val look = FrameLooks.forLevel(dog.minLevel)
            row.portrait.setImageResource(dog.portraitRes)
            row.portrait.alpha = if (open) 1f else 0.28f
            row.ring.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (open) look.glow else 0x22A89880)
                setStroke((if (open) look.widthDp else 3f).dp(), if (open) look.ring else 0xFFC4B6A6.toInt())
            }
            val name = if (open) petProgress.displayName(dog.minLevel, getString(dog.nameRes)) else getString(dog.nameRes)
            row.name.text = name
            row.breed.setText(dog.titleRes)
            row.date.text = when {
                !open -> getString(R.string.dog_locked, dog.minLevel)
                dog.minLevel == selected -> getString(
                    R.string.collection_home,
                    petProgress.formatUnlock(petProgress.unlockDate(dog.minLevel))
                )
                else -> getString(R.string.collection_got, petProgress.formatUnlock(petProgress.unlockDate(dog.minLevel)))
            }
            row.card.alpha = if (open) 1f else 0.72f
            if (!open) {
                row.card.setOnClickListener {
                    Toast.makeText(this, getString(R.string.dog_locked, dog.minLevel), Toast.LENGTH_SHORT).show()
                }
            } else {
                row.card.setOnClickListener {
                    petProgress.setSelectedLevel(dog.minLevel)
                    Toast.makeText(this, getString(R.string.collection_selected, name), Toast.LENGTH_SHORT).show()
                    showDogs(level)
                }
                row.name.setOnClickListener { rename(dog.minLevel, getString(dog.nameRes)) }
            }
        }
    }

    private fun rename(minLevel: Int, fallback: String) {
        val input = EditText(this).apply {
            setText(petProgress.displayName(minLevel, fallback))
            setSelection(text.length)
            setSingleLine()
            setTextColor(Color.parseColor("#3B2918"))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_rename_pet)
            .setView(input)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save_name) { _, _ ->
                petProgress.setName(minLevel, input.text?.toString().orEmpty())
                showDogs(petProgress.level())
            }
            .show()
    }

    private fun Float.dp(): Int = (this * resources.displayMetrics.density).toInt().coerceAtLeast(1)
}
