package com.rgdmenu.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rgdmenu.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val items = listOf(
            MenuItem("Margherita Pizza", "Tomato, mozzarella, basil", "$9.50"),
            MenuItem("Cheeseburger", "Beef patty, cheddar, lettuce, tomato", "$8.00"),
            MenuItem("Caesar Salad", "Romaine, parmesan, croutons, Caesar dressing", "$6.50"),
            MenuItem("Spaghetti Carbonara", "Egg, pancetta, parmesan, black pepper", "$10.00"),
            MenuItem("Iced Coffee", "Cold brew coffee over ice", "$3.50")
        )

        binding.recyclerMenu.layoutManager = LinearLayoutManager(this)
        binding.recyclerMenu.adapter = MenuAdapter(items)
    }
}
