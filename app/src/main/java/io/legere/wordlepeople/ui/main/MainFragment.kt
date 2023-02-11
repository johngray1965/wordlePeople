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
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import io.legere.wordlepeople.db.entity.Color
import io.legere.wordlepeople.db.entity.Gender
import io.legere.wordlepeople.R
import io.legere.wordlepeople.databinding.FragmentMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

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

        adapter = WordlePeopleAdapter(inflater = inflater)
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

        setupMenu()
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
                    R.id.action_import_data -> {
                        handleImport()
                    }
                    R.id.action_clear_all -> {
                        viewModel.clearAll()
                    }
                    R.id.action_clear_color_filter -> {
                        viewModel.clearColor()
                    }
                    R.id.action_red_color_filter -> {
                        flip(menuItem)
                        viewModel.setRedFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_green_color_filter -> {
                        flip(menuItem)
                        viewModel.setGreenFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_blue_color_filter -> {
                        flip(menuItem)
                        viewModel.setBlueFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_clear_gender_filter -> {
                        viewModel.clearGender()
                    }
                    R.id.action_north_gender_filter -> {
                        flip(menuItem)
                        viewModel.setNorthFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_south_gender_filter -> {
                        flip(menuItem)
                        viewModel.setSouthFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_east_gender_filter -> {
                        flip(menuItem)
                        viewModel.setEastFilter(
                            menuItem.isChecked
                        )
                    }
                    R.id.action_west_gender_filter -> {
                        flip(menuItem)
                        viewModel.setWestFilter(
                            menuItem.isChecked
                        )
                    }
                }
                return true
            }


        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun handleImport() {
        val snack: Snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
        snack.show()
        lifecycleScope.launch {
            viewModel.loadData().collect {
                snack.setText(it)
            }
            snack.dismiss()
        }
    }

    private fun flip(menuItem: MenuItem) {
        if (menuItem.isCheckable) {
            menuItem.isChecked = !menuItem.isChecked
        }

    }

}