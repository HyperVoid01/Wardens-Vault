package com.soo.wardensvault


import android.content.Context
import android.content.Intent
import android.os.Bundle


fun openIntent(context: Context, order: String, activityToOpen: Class<*>){
    //declare intent with context and class to pass value to
    val intent = Intent(context, activityToOpen)
    //pass through the string value with key "order"
    intent.putExtra("order" , order)
    //if the context is not an activity
    if (context !is android.app.Activity){
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    //start the activity
    context.startActivity(intent)
}

fun shareIntent(context: Context, order: String){
    val sendIntent = Intent();
    //setting the action to tell it what to do
    sendIntent.setAction(Intent.ACTION_SEND)
    sendIntent.putExtra(Intent.EXTRA_TEXT, order)
    //we are sending plain text
    sendIntent.setType("text/plain")
    //show the share intent
    val shareIntent = Intent.createChooser(sendIntent,null)
    //if the context is not an activity
    if (context !is android.app.Activity){
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(shareIntent)
}

//fun shareIntent(context: Context, order: Order){
//    val sendIntent = Intent()
//    sendIntent.setAction(Intent.ACTION_SEND)
//
//    //creating bundle to store/add multiple values
//    val shareOrdrDetails = Bundle()
//    shareOrdrDetails.putString("productName", order.productName)
//    shareOrdrDetails.putString("customrName", order.customerName)
//    shareOrdrDetails.putString("customerCell", order.customerCell)
//
//    //share the entire bundle
//    sendIntent.putExtra(Intent.EXTRA_TEXT, shareOrdrDetails)
//    sendIntent.setType("text/plain")
//
//    val shareIntent = Intent.createChooser(sendIntent,null)
//
//    //if the context is not an activity
//    if (context !is android.app.Activity){
//        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//    }
//    context.startActivity(shareIntent)
//
//}