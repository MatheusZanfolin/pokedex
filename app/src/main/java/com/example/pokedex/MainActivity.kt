package com.example.pokedex

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.DialogInterface
import android.databinding.DataBindingUtil
import android.net.ConnectivityManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.pokedex.databinding.ActivityMainBinding
import com.example.pokedex.databinding.DlgSearchPkmnBinding
import com.example.pokedex.model.Pokemon
import com.example.pokedex.model.PokemonListAdapter
import com.example.pokedex.task.GetPokemonMethod
import com.example.pokedex.task.GetPokemonTask
import com.example.pokedex.task.SearchPokemonTask
import com.example.pokedex.viewmodel.PokemonListViewModel
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {
    // TODO Separate list into tabs, each containing a generation

    val INITIAL_POKEMON_COUNT = 9

    lateinit var binding: ActivityMainBinding

    var pokemonSearchResult: Pokemon? = null

    companion object {
        var stopAllThreads: Boolean = false
    }

    enum class PokemonSearchType {
        BY_ID,
        BY_NAME
    }

    val model: PokemonListViewModel by lazy {
        ViewModelProviders.of(this).get(PokemonListViewModel::class.java)
    }

    val listAdapter: PokemonListAdapter
        get() = binding.lstPokemons.adapter as PokemonListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        stopAllThreads = false

        setUpViewModel()

        setUpList()

        enableContinuousLoading()
    }

    override fun onResume() {
        super.onResume()

        stopAllThreads = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.btnSearch -> {
                AlertDialog.Builder(this)
                    .setTitle("Buscar pokémon")
                    .setItems(R.array.searchTypes, getSearchTypeListener())
                    .create()
                    .show()

                return true
            }
        }

        return false
    }

    private fun getSearchTypeListener(): DialogInterface.OnClickListener? {
        return DialogInterface.OnClickListener { dialog, which ->
            when (which) {
                0 -> showSearchDialog("Nome do pokémon", PokemonSearchType.BY_NAME)
                1 -> showSearchDialog("ID do pokémon", PokemonSearchType.BY_ID)
            }
        }
    }

    private fun showSearchDialog(hint: String, searchType: PokemonSearchType) {
        val searchView: View = LayoutInflater.from(this).inflate(R.layout.dlg_search_pkmn, null, false)

        val binding = DlgSearchPkmnBinding.bind(searchView)

        binding.edSearch.hint = hint
        binding.edSearch.inputType = if (searchType == PokemonSearchType.BY_ID) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Buscar pokémon")
            .setView(searchView)
            .setPositiveButton("PESQUISAR", getSearchLisneter(binding, searchType))
            .create()
            .show()
    }

    private fun getSearchLisneter(binding: DlgSearchPkmnBinding, searchType: PokemonSearchType): DialogInterface.OnClickListener? {
        return DialogInterface.OnClickListener { dialog, which ->
            SearchPokemonTask(searchType, WeakReference(this)).execute(binding.edSearch.text.toString())
        }
    }

    private fun setUpViewModel() {
        val pokemonObserver = Observer<Pokemon> { newPokemon ->
            newPokemon?.let {
                listAdapter.addPokemon(newPokemon)
            }
        }

        for (data in model.pokemonsListData)
            data.observe(this, pokemonObserver)
    }

    private fun setUpList() {
        binding.lstPokemons.adapter = PokemonListAdapter { pkmn -> onPokemonClick(pkmn) }
        binding.lstPokemons.layoutManager = LinearLayoutManager(this)
        binding.lstPokemons.addItemDecoration(getDecoration(getLayoutManager()))
        binding.lstPokemons.itemAnimator = DefaultItemAnimator()
    }

    private fun onPokemonClick(pokemon: Pokemon) {
        Toast.makeText(this, "Clicou em $pokemon", Toast.LENGTH_SHORT).show()
    }

    private fun enableContinuousLoading() {
        if (isOnline()) {
            GetPokemonTask(GetPokemonMethod.ASYNCHRONOUS, 1, INITIAL_POKEMON_COUNT, model, WeakReference(binding.loadingScreen)).execute()
        } else {
            showNoInternetDialog()
        }
    }

    private fun showNoInternetDialog() {
        if (isUiThread()) {
            AlertDialog.Builder(this)
                .setTitle("Sem internet!")
                .setMessage("Por favor, conecte-se à internet e reinicie sua Pokédex!")
                .setPositiveButton("REINICIAR", getRestartButtonListener())
                .setNegativeButton("SAIR", getExitButtonListener())
                .create()
                .show()
        }
    }

    private fun isUiThread(): Boolean {
        return Looper.getMainLooper().thread == Thread.currentThread()
    }

    private fun getDecoration(layoutManager: LinearLayoutManager): RecyclerView.ItemDecoration {
        return DividerItemDecoration(this, layoutManager.orientation)
    }

    private fun getLayoutManager(): LinearLayoutManager {
        return binding.lstPokemons.layoutManager as LinearLayoutManager
    }

    private fun getExitButtonListener(): DialogInterface.OnClickListener? {
        return DialogInterface.OnClickListener { dialog, which ->
            finish()
        }
    }

    private fun getRestartButtonListener(): DialogInterface.OnClickListener? {
        return DialogInterface.OnClickListener { dialog, which ->
            finish()
            startActivity(intent)
        }
    }

    private fun isOnline(): Boolean {
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val netInfo = connManager.activeNetworkInfo

        return netInfo?.isConnected ?: false
    }

    override fun onBackPressed() {
        stopAllThreads = true

        finishAffinity()
    }
}
