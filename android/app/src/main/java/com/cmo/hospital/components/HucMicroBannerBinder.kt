package com.cmo.hospital.components

import com.cmo.hospital.databinding.CompHucMicroBannerBinding

object HucMicroBannerBinder {
    fun bind(
        binding: CompHucMicroBannerBinding,
        title: String,
        subtitle: String,
        badgeText: String = "Online",
        iconRes: Int? = null
    ) {
        binding.tvBannerTitle.text = title
        binding.tvBannerSubtitle.text = subtitle
        binding.tvBannerBadge.text = badgeText
        if (iconRes != null) {
            binding.ivBannerIcon.setImageResource(iconRes)
        }
    }
}
