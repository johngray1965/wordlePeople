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
    private val listener: OnItemInteractionListener,
) :
    PagingDataAdapter<WordlePerson, ViewHolder>(WORDLE_PERSON_DIFFER) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it, listener) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PersonRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }
}

private val WORDLE_PERSON_DIFFER = object: DiffUtil.ItemCallback<WordlePerson>() {
    override fun areItemsTheSame(oldItem: WordlePerson, newItem: WordlePerson): Boolean =
        oldItem.id == newItem.id

    /**
     * Important - WordlePeople are immutable.  There's no way to change one (at this time)
     * What's important is to compare here are fields that are mutable and effect the display
     * state (fields we display or otherwise use to change how the item is displayed)
     *
     * At the moment nothing is mutable, so we always return true
     */
    override fun areContentsTheSame(oldItem: WordlePerson, newItem: WordlePerson): Boolean {
        return true
    }
}

class ViewHolder(private val binding: PersonRowBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(wordlePerson: WordlePerson, listener: OnItemInteractionListener) {
        binding.colorView.setBackgroundResource(when(wordlePerson.color) {
            Color.Blue -> R.color.blue
            Color.Green -> R.color.green
            Color.Red -> R.color.red
        })
        binding.nameTextView.text = getName(wordlePerson)
        binding.genderTextView.text = wordlePerson.gender.name
        binding.imageButton.setOnClickListener {
            listener.onItemDelete(wordlePerson)
        }
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

interface OnItemInteractionListener {
    fun onItemDelete(wordlePerson: WordlePerson)
}
