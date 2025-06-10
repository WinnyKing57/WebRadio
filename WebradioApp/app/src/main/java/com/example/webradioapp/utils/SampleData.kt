package com.example.webradioapp.utils

import com.example.webradioapp.model.RadioStation

object SampleData {

    val stations = listOf(
        RadioStation(
            id = "1",
            name = "Radio Paradise (FLAC)",
            streamUrl = "https://stream.radioparadise.com/flac",
            genre = "Eclectic Rock",
            country = "USA",
            favicon = "https://radioparadise.com/graphics/favicon_RP_32.png"
        ),
        RadioStation(
            id = "2",
            name = "Radio Paradise (Mellow Mix FLAC)",
            streamUrl = "https://stream.radioparadise.com/mellow-flac",
            genre = "Mellow Eclectic Rock",
            country = "USA",
            favicon = "https://radioparadise.com/graphics/favicon_RP_mellow_32.png"
        ),
        RadioStation(
            id = "3",
            name = "Lofi Girl",
            streamUrl = "https://play.streamafrica.net/lofiradio", // Common Lofi stream, check validity
            genre = "Lofi Hip Hop",
            country = "France",
            favicon = "https://lofigirl.com/wp-content/uploads/2021/08/cropped-Lofi-Girl-favicon-32x32.png"
        ),
        RadioStation(
            id = "4",
            name = "NTS Radio 1",
            streamUrl = "https://stream-relay-geo.ntslive.net/stream",
            genre = "Alternative/Electronic",
            country = "UK",
            favicon = "https://www.nts.live/favicon.ico"
        ),
        RadioStation(
            id = "5",
            name = "BBC Radio 6 Music",
            streamUrl = "http://stream.live.bbc.co.uk/bbc_6music_aac_heaac_sbr_cloud", // AAC stream
            genre = "Alternative Rock/Pop",
            country = "UK",
            favicon = "https://www.bbc.co.uk/favicon.ico"
        )
    )
}
