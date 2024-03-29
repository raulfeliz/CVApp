package com.raul.androidapps.cvapp.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.AppBarLayout
import com.raul.androidapps.cvapp.R
import com.raul.androidapps.cvapp.databinding.CVAppBindingComponent
import com.raul.androidapps.cvapp.databinding.MainActivityBinding
import com.raul.androidapps.cvapp.network.Resource
import com.raul.androidapps.cvapp.permission.PermissionManager
import com.raul.androidapps.cvapp.resources.ResourcesManagerImpl
import com.raul.androidapps.cvapp.ui.common.CVAppViewModelFactory
import com.raul.androidapps.cvapp.utils.ViewUtils
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject
import kotlin.math.abs


class MainActivity : DaggerAppCompatActivity(), AppBarLayout.OnOffsetChangedListener {

    private lateinit var binding: MainActivityBinding
    private lateinit var viewModel: MainViewModel
    private val permissionCode: Int = 999

    @Inject
    lateinit var viewModelFactory: CVAppViewModelFactory

    @Inject
    lateinit var resourcesManager: ResourcesManagerImpl

    @Inject
    lateinit var viewUtils: ViewUtils

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var cvAppBindingComponent: CVAppBindingComponent

    private var originalProfilePicHeight = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.main_activity, cvAppBindingComponent)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel::class.java)
        binding.appbarLayoutMainActivity?.addOnOffsetChangedListener(this)

        binding.bottomNavigation.apply {
            this.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (this@apply.height > 0) {
                        addMarginToFragmentContainer()
                        this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            })
        }


        setToolbar(binding.toolbarMainActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateStatusBar(resourcesManager.getColor(android.R.color.white))
        }

        findNavController(this, R.id.fragment_container).apply {
            addOnDestinationChangedListener { _, destination, _ ->
                binding.appbarLayoutMainActivity?.setExpanded(destination.id == R.id.info_fragment)
            }
            binding.bottomNavigation.setupWithNavController(this)
        }

        viewModel.getProfile().observe({ lifecycle }) {
            it?.let {
                binding.profile = it
                binding.profilePicLayer.visibility = View.VISIBLE
                setTitle(it.name)
            }
        }

        viewModel.getLoadingState().observe({ lifecycle }) {
            it?.let { resource ->
                when (resource.status) {
                    Resource.Status.SUCCESS -> hideLoading()
                    Resource.Status.ERROR -> {
                        hideLoading()
                        showError(resource.message)
                    }
                    Resource.Status.LOADING -> showLoading()
                }
            }
        }

    }

    override fun onSupportNavigateUp() =
        findNavController(this, R.id.fragment_container).navigateUp()

    private fun setToolbar(toolbar: Toolbar, title: String? = null) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowTitleEnabled(true)
            this.title = title
        }
    }

    private fun setTitle(title: String) {
        binding.collapsingToolbarMainActivity?.title = title
        supportActionBar?.title = title
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun updateStatusBar(color: Int) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val setStatusBarThemeAsDark =
                if (color == resourcesManager.getColor(android.R.color.transparent)) {
                    false
                } else {
                    viewUtils.hasEnoughContrast(color)
                }
            setSystemBarTheme(setStatusBarThemeAsDark)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun setSystemBarTheme(darkBackgroundTheme: Boolean) {
        val lFlags = this.window.decorView.systemUiVisibility
        this.window.decorView.systemUiVisibility =
            if (darkBackgroundTheme) lFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() else lFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun showLoading() {
        binding.progressCircular.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.progressCircular.visibility = View.GONE
    }

    private fun showError(message: String?) {
        message?.let {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }


    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {

        if (originalProfilePicHeight < 0) {
            originalProfilePicHeight = binding.profilePic.height
        }

        val percentage = abs(verticalOffset.toFloat() / appBarLayout.totalScrollRange)

        val finalHeight =
            binding.toolbarMainActivity.height - viewUtils.getPxFromDp(resourcesManager, 16f)
        val diff = originalProfilePicHeight - finalHeight
        val paramsProfilePic = binding.profilePic.layoutParams
        paramsProfilePic.height = (originalProfilePicHeight - diff * percentage).toInt()
        paramsProfilePic.width = paramsProfilePic.height
        binding.profilePic.requestLayout()
        val paramsProfilePicLayer = binding.profilePicLayer.layoutParams
        val frameExtraWidth = viewUtils.getPxFromDp(resourcesManager, 10f)
        paramsProfilePicLayer.height = paramsProfilePic.height + frameExtraWidth.toInt()
        paramsProfilePicLayer.width = paramsProfilePicLayer.height
        binding.profilePicLayer.requestLayout()

        binding.backgroundPic.alpha = 0.3f - percentage
        binding.profilePicLayer.alpha = 1 - percentage
        binding.mailText.alpha = 1 - percentage
        binding.mailIcon.alpha = 1 - percentage
        binding.phoneText.alpha = 1 - percentage
        binding.phoneIcon.alpha = 1 - percentage
        binding.linkedinIcon.alpha = 1 - percentage
        binding.linkedinText.alpha = 1 - percentage
        binding.githubIcon.alpha = 1 - percentage
        binding.githubText.alpha = 1 - percentage

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            permissionCode -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    launchDialer()
                }
            }
        }
    }

    fun sendMail(v: View) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:") // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf((v as? TextView)?.text))
        intent.putExtra(
            Intent.EXTRA_SUBJECT,
            resourcesManager.getString(R.string.mail_subject)
        )
        startActivity(intent)
    }

    @Suppress("UNUSED_PARAMETER")
    fun callNumber(v: View) {
        permissionManager.requestPermissions(
            activity = this,
            callbackAllPermissionsGranted = { launchDialer() },
            permissions = listOf(Manifest.permission.CALL_PHONE),
            messageRationale = null,
            showRationaleMessageIfNeeded = false,
            code = permissionCode
        )
    }

    @SuppressLint("MissingPermission")
    fun launchDialer() {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:${binding.phoneText.text}")
        startActivity(callIntent)
    }

    private fun addMarginToFragmentContainer() {
        (binding.mainContainer?.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.bottomMargin = binding.bottomNavigation.height
            binding.mainContainer?.requestLayout()
        }
    }


}

