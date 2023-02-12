package io.legere.wordlepeople.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.Gender
import io.legere.wordlepeople.R
import io.legere.wordlepeople.databinding.FragmentMainBinding
import io.legere.wordlepeople.db.entity.WordlePerson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ln
import kotlin.math.pow

class MainFragment : Fragment(), OnItemInteraction {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentMainBinding
    private lateinit var adapter: WordlePeopleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        adapter = WordlePeopleAdapter(inflater = inflater, this)
        binding.recyclerview.adapter = adapter
        binding.recyclerview.setHasFixedSize(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            viewModel.wordlePeopleFlow.collectLatest { pagingData ->
                adapter.submitData(pagingData)
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.countFlow.collectLatest {
                (activity as? AppCompatActivity)?.supportActionBar?.title =
                    when (it.first) {
                        it.second -> getString(R.string.world_people_with_count, getFormattedNumber(it.first))
                        else -> getString(
                            R.string.world_people_with_count_of,
                            getFormattedNumber(it.second),
                            getFormattedNumber(it.first)
                        )
                    }

            }
        }

        setupMenu()
    }

    @Suppress("MagicNumber", "ImplicitDefaultLocale")
    fun getFormattedNumber(count: Int): String {
        if (count < 1000) return "" + count
        val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
        return String.format("%.1f%c", count / 1000.0.pow(exp.toDouble()), "kMGTPE"[exp - 1])
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object: MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_filter, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                menu.findItem(R.id.action_red_color_filter).isChecked = viewModel.colors.contains(
                    Color.Red)
                menu.findItem(R.id.action_green_color_filter).isChecked = viewModel.colors.contains(
                    Color.Green)
                menu.findItem(R.id.action_blue_color_filter).isChecked = viewModel.colors.contains(
                    Color.Blue)

                menu.findItem(R.id.action_north_gender_filter).isChecked = viewModel.genders.contains(
                    Gender.North)
                menu.findItem(R.id.action_south_gender_filter).isChecked = viewModel.genders.contains(
                    Gender.South)
                menu.findItem(R.id.action_east_gender_filter).isChecked = viewModel.genders.contains(
                    Gender.East)
                menu.findItem(R.id.action_west_gender_filter).isChecked = viewModel.genders.contains(
                    Gender.West)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when(menuItem.itemId) {
                    R.id.action_import_data -> handleImport()
                    R.id.action_clear_data -> viewModel.clearData()
                    R.id.action_clear_filter -> viewModel.clearAll()
                    R.id.action_clear_color_filter -> viewModel.clearColor()
                    R.id.action_red_color_filter ->  viewModel.setRedFilter(flip(menuItem))
                    R.id.action_green_color_filter -> viewModel.setGreenFilter(flip(menuItem))
                    R.id.action_blue_color_filter -> viewModel.setBlueFilter(flip(menuItem))
                    R.id.action_clear_gender_filter ->  viewModel.clearGender()
                    R.id.action_north_gender_filter -> viewModel.setNorthFilter(flip(menuItem))
                    R.id.action_south_gender_filter -> viewModel.setSouthFilter(flip(menuItem))
                    R.id.action_east_gender_filter ->  viewModel.setEastFilter(flip(menuItem))
                    R.id.action_west_gender_filter -> viewModel.setWestFilter(flip(menuItem))
                }
                return true
            }


        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleImport() {
        lifecycleScope.launch {
            displayItemCountChoiceDialogAsync().await()?.let { count ->
                val snack: Snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
                snack.show()
                viewModel.loadData(count).collect {
                    snack.setText(it)
                }
                snack.dismiss()
            }
        }
    }

    @Suppress("MagicNumber")
    private fun displayItemCountChoiceDialogAsync(): Deferred<Int?> = lifecycleScope.async {
        val valueToHumanString = mapOf(
            1000 to "1,000",
            5000 to "5,000",
            10000 to "10,000",
            25000 to "25,000",
            50000 to "50,000",
            100000 to "100,000"
        )

        val selected = suspendCoroutine { continuation ->
            AlertDialog.Builder(requireContext())
                .setTitle("Items to import")
                .setSingleChoiceItems(
                    valueToHumanString.values.toTypedArray(),
                    -1 // no selection
                ) { dialog, which ->
                    dialog.dismiss()
                    continuation.resume(which)
                }
                .setNegativeButton(android.R.string.cancel)
                { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(-1)
                }
                .show()
        }
        if (selected == -1)
            null
        else
            valueToHumanString.keys.elementAtOrNull(selected)
    }

    private fun flip(menuItem: MenuItem): Boolean {
        if (menuItem.isCheckable) {
            menuItem.isChecked = !menuItem.isChecked
            return menuItem.isChecked
        }
        return false

    }

    override fun onItemDelete(wordlePerson: WordlePerson) {
        viewModel.delete(wordlePerson)
    }

}
