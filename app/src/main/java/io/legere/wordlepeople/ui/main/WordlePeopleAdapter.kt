package io.legere.wordlepeople.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legere.wordlepeople.R
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.WordlePerson
import io.legere.wordlepeople.databinding.PersonRowBinding
import java.util.Locale

class WordlePeopleAdapter(
    private val inflater: LayoutInflater,
) :
    PagingDataAdapter<WordlePerson, ViewHolder>(WORLDE_PERSON_DIFFER) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PersonRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }
}

private val WORLDE_PERSON_DIFFER = object: DiffUtil.ItemCallback<WordlePerson>() {
    override fun areItemsTheSame(oldItem: WordlePerson, newItem: WordlePerson): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: WordlePerson, newItem: WordlePerson): Boolean =
        oldItem == newItem

}

class ViewHolder(private val binding: PersonRowBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(wordlePerson: WordlePerson) {
        binding.colorView.setBackgroundResource(when(wordlePerson.color) {
            Color.Blue -> R.color.blue
            Color.Green -> R.color.green
            Color.Red -> R.color.red
        })
        binding.nameTextView.text = getName(wordlePerson)
        binding.genderTextView.text = wordlePerson.gender.name
    }

    private fun getName(wordlePerson: WordlePerson) : String {
        val last = capitalize(wordlePerson.lastName)
        val first = capitalize(wordlePerson.firstName)
        val middleInitial = wordlePerson.middleName.first().titlecase(Locale.ROOT)
        return "$last, $first $middleInitial."
    }

    private fun capitalize(s: String) =
        s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()  }

}