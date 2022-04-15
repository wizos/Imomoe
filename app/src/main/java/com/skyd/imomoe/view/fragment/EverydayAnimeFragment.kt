package com.skyd.imomoe.view.fragment

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.skyd.imomoe.R
import com.skyd.imomoe.databinding.FragmentEverydayAnimeBinding
import com.skyd.imomoe.ext.hideToolbarWhenCollapsed
import com.skyd.imomoe.util.eventbus.EventBusSubscriber
import com.skyd.imomoe.util.eventbus.MessageEvent
import com.skyd.imomoe.util.eventbus.RefreshEvent
import com.skyd.imomoe.view.adapter.variety.VarietyAdapter
import com.skyd.imomoe.view.adapter.variety.proxy.AnimeCover12Proxy
import com.skyd.imomoe.view.adapter.variety.proxy.GridRecyclerView1Proxy
import com.skyd.imomoe.view.component.WrapLinearLayoutManager
import com.skyd.imomoe.view.listener.dsl.addOnTabSelectedListener
import com.skyd.imomoe.viewmodel.EverydayAnimeViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class EverydayAnimeFragment : BaseFragment<FragmentEverydayAnimeBinding>(), EventBusSubscriber {
    private val viewModel: EverydayAnimeViewModel by viewModels()
    private val adapter: VarietyAdapter by lazy {
        VarietyAdapter(
            mutableListOf(
                GridRecyclerView1Proxy(
                    onBindViewHolder = { holder, data, _ ->
                        val rvLayoutParams = holder.rvGridRecyclerView1.layoutParams
                        rvLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        holder.rvGridRecyclerView1.layoutManager =
                            WrapLinearLayoutManager(requireActivity())
                        holder.rvGridRecyclerView1.layoutParams = rvLayoutParams
                        holder.rvGridRecyclerView1.isNestedScrollingEnabled = true
                        val adapter = VarietyAdapter(
                            mutableListOf(AnimeCover12Proxy())
                        ).apply { dataList = data }
                        holder.rvGridRecyclerView1.adapter = adapter
                    },
                    height = ViewGroup.LayoutParams.MATCH_PARENT,
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        )
    }
    private var offscreenPageLimit = 1
    private var selectedTabIndex = -1
    private var lastRefreshTime: Long = System.currentTimeMillis()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentEverydayAnimeBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.run {
            vp2EverydayAnimeFragment.offscreenPageLimit = offscreenPageLimit
            srlEverydayAnimeFragment.setOnRefreshListener { refresh() }

            tlEverydayAnimeFragment.addOnTabSelectedListener {
                onTabSelected { tab -> selectedTabIndex = tab?.position ?: return@onTabSelected }
            }
            vp2EverydayAnimeFragment.adapter = adapter

            ablEverydayAnimeFragment.hideToolbarWhenCollapsed(tbEverydayAnimeFragment)

            val tabLayoutMediator = TabLayoutMediator(
                mBinding.tlEverydayAnimeFragment,
                mBinding.vp2EverydayAnimeFragment//.getViewPager()
            ) { tab, position ->
                tab.text = viewModel.mldTabList.value?.get(position)?.title
            }
            tabLayoutMediator.attach()
        }

        viewModel.mldHeader.observe(viewLifecycleOwner) {
            mBinding.tbEverydayAnimeFragment.title = it
        }

        viewModel.mldEverydayAnimeList.observe(viewLifecycleOwner) {
            mBinding.srlEverydayAnimeFragment.closeHeaderOrFooter()//.isRefreshing = false

            if (it != null) {
                val selectedTabIndex = this.selectedTabIndex
                activity?.let { _ ->
                    //先隐藏
                    ObjectAnimator.ofFloat(mBinding.vp2EverydayAnimeFragment, "alpha", 1f, 0f)
                        .setDuration(270).start()
                    adapter.dataList = it

                    val tabCount = adapter.itemCount
                    mBinding.vp2EverydayAnimeFragment.post {
                        if (selectedTabIndex != -1 && selectedTabIndex < tabCount)
                            mBinding.vp2EverydayAnimeFragment.setCurrentItem(
                                selectedTabIndex, false
                            )
                        else if (selectedTabIndex == -1 && viewModel.selectedTabIndex < tabCount
                            && viewModel.selectedTabIndex >= 0
                        ) {
                            mBinding.vp2EverydayAnimeFragment.setCurrentItem(
                                viewModel.selectedTabIndex, false
                            )
                        }
                        //设置完数据后显示，避免闪烁
                        ObjectAnimator.ofFloat(mBinding.vp2EverydayAnimeFragment, "alpha", 0f, 1f)
                            .setDuration(270).start()
                    }
                }
                hideLoadFailedTip()
            } else {
                adapter.dataList = emptyList()
                showLoadFailedTip(getString(R.string.load_data_failed_click_to_retry)) {
                    viewModel.getEverydayAnimeData()
                    hideLoadFailedTip()
                }
            }
        }

        if (viewModel.mldTabList.value == null || viewModel.mldEverydayAnimeList.value == null) {
            mBinding.srlEverydayAnimeFragment.autoRefresh()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(event: MessageEvent) {
        when (event) {
            is RefreshEvent -> {
                refresh()
            }
        }
    }

    private fun refresh() {
        //避免刷新间隔太短
        if (System.currentTimeMillis() - lastRefreshTime > 500) {
            lastRefreshTime = System.currentTimeMillis()
            viewModel.getEverydayAnimeData()
        } else {
            mBinding.srlEverydayAnimeFragment.closeHeaderOrFooter()
        }
    }

    override fun getLoadFailedTipView(): ViewStub = mBinding.layoutEverydayAnimeFragmentLoadFailed
}
