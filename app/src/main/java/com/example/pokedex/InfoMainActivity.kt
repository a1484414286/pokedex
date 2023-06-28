package com.example.pokedex

import com.example.pokedex.evolution.PokeEvo
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.pokedex.main.Ability
import com.example.pokedex.swipes.PageAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


@Suppress("UNCHECKED_CAST")
class InfoMainActivity : AppCompatActivity() {
    private lateinit var viewPager : ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pageAdapter : PageAdapter
    private lateinit var evolutionList : ArrayList<PokeEvo>
    private lateinit var abilitiesList : ArrayList<Ability>
    private lateinit var aboutStats : HashMap<String,Any>

    private lateinit var id : String
    private lateinit var name : String
    private lateinit var type1 : String
    private lateinit var type2 : String
    private val database = Firebase.database


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.status_page)
        evolutionList = ArrayList()
        abilitiesList = ArrayList()
        aboutStats = HashMap()
        receiveDataFromPreviousActivity()
        tabsContentSwitch()
    }

    private fun loadDataFromDB(
        index: Int,
        callback: (
            abilities: Map<String, Boolean>, base_exp: Int, effort: Map<String, Long>, moves: Map<String, Map<String, Any>>, stats: Map<String, Long>,
            height: Int, weight: Int, evolutionMap: Any,
        ) -> Unit,
    ) {
        val myRef = database.getReference("$index")

        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value =
                    dataSnapshot.value as HashMap<*, *>? // Retrieve the value from the snapshot
                val abilities = value?.get("abilities") as? Map<String,Boolean>
                val base_exp = value!!["base_exp"].toString().toInt()
                val effort = value["effort"] as? Map<String, Long>
                val moves = value["moves"] as Map<String, Map<String, Any>>
                val stats = value["stats"] as Map<String, Long>
                val height = (value["height"] as Long).toInt()
                val weight = (value["weight"] as Long).toInt()
                val evolutionMap : Any
                evolutionMap = if(index < 4) {
                    value["evolution"] as ArrayList<Any>

                } else {
                    value["evolution"] as HashMap<Int,Any>
                }

                callback(
                    abilities!!, base_exp,
                    effort!! , moves, stats, height, weight, evolutionMap
                )
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
            }
        })
    }

    @SuppressLint("DiscouragedApi")
    private fun fetchResourceID(drawableName : String): Int {
        var drawableResourceId = resources.getIdentifier("p$drawableName", "drawable", packageName)
        if (drawableResourceId == 0) {
            val name = "p${drawableName}_f"
            drawableResourceId =
                resources.getIdentifier(name, "drawable", packageName)
        }
        return drawableResourceId
    }
    @SuppressLint("SetTextI18n")
    private fun receiveDataFromPreviousActivity()
    {
        id = intent.getStringExtra("id")!!
        name = intent.getStringExtra("name")!!
        type1 = intent.getStringExtra("type1")!!
        type2 = intent.getStringExtra("type2") ?: ""
        id = id.replace("§","").trim()
        val imageView = findViewById<ImageView>(R.id.pokedexAvatar)
        findViewById<TextView>(R.id.pokedexID).text = "§  ${this.id}"
        findViewById<TextView>(R.id.pokedexPokemonName).text = name
        val drawableResourceId= fetchResourceID(id)
        Glide.with(this)
            .load(drawableResourceId)
            .into(imageView)

        loadDataFromDB(id.toInt()) { abilities, XP, effort, moves, stats, height, weight, evolutionMap->
            // update ui based on data called back from fetch

            aboutStats["base_exp"] = XP
            aboutStats["height"] = height
            aboutStats["weight"] = weight

            abilities.let { list ->
                for(ability in list.keys) {
                    val name =  ability as String
                    val bool = list[ability] as Boolean
                    abilitiesList.add(Ability(name,bool))
                }
            }

            effort.let { effortMap ->
                for(key in effortMap.keys) {
                    val name = key
                    val value = effortMap[key]?.toInt()
                }
            }

            moves.let { movesMap ->
                for(key in movesMap.keys) {
                    val name = key
                    val move = movesMap[key]
                    val accuracy = move?.get("accuracy")
                    val lvlReq = move?.get("lvl")
                    val power = move?.get("power")
                    val pp = move?.get("pp")
                    val type = move?.get("type")
                }
            }

            stats.let { statsMap ->
                for(key in statsMap.keys) {
                    val name = key
                    val value = statsMap[key]
                }
            }


            if(evolutionMap is HashMap<*,*>)
            {
                for(key in evolutionMap.keys)
                {
                    val id = key.toString().toInt()
                    val detailedMap = evolutionMap[key] as HashMap<*,*>
                    evolutionList.add(PokeEvo(
                        fetchResourceID(key.toString()), detailedMap["minLevel"] as Long,
                    detailedMap["trigger"] as String, detailedMap["priority"] as Long
                    ))
                }
            }

            else if(evolutionMap is ArrayList<*>)
            {
                for(i in 1 until evolutionMap.size)
                {
                    val detailedMap = evolutionMap[i] as HashMap<*,*>
                    evolutionList.add( PokeEvo(fetchResourceID(i.toString()), detailedMap["minLevel"] as Long,
                    detailedMap["trigger"] as String, detailedMap["priority"] as Long
                ))
                }

            }
            // Use other retrieved values (baseExp, effort, moves, stats) as needed
        }
    }



    @SuppressLint("UseCompatLoadingForDrawables")
    private fun tabsContentSwitch()
    {

        val button = findViewById<ImageButton>(R.id.genderSwitch)
        button.setImageDrawable(getDrawable(R.drawable.gender_switch))
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        pageAdapter = PageAdapter(supportFragmentManager,lifecycle,evolutionList,abilitiesList,aboutStats)
        viewPager.adapter = pageAdapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Called when a tab is unselected
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Called when a tab is reselected (tab is already selected)
            }
        })

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
    }
}