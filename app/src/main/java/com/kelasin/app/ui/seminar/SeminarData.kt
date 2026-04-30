package com.kelasin.app.ui.seminar

import androidx.compose.ui.graphics.Color

data class SeminarInfo(
    val title: String,
    val shortDescription: String,
    val fullDescription: String,
    val date: String,
    val location: String,
    val quota: Int,
    val registeredCount: Int = 0,
    val remainingQuota: Int = quota,
    val isFull: Boolean = false,
    val speaker: String,
    val category: String,
    val accentColor: Color
)

object SeminarData {

    val SEMINARS: List<SeminarInfo> = listOf(
        SeminarInfo(
            title = "AI & Future Technology Summit",
            shortDescription = "Eksplorasi tren AI terkini dan dampaknya terhadap dunia kerja masa depan.",
            fullDescription = "Seminar ini membahas perkembangan kecerdasan buatan (AI), machine learning, dan otomasi industri. Peserta akan mendapatkan wawasan mendalam tentang bagaimana AI mengubah lanskap karir dan peluang bagi mahasiswa teknologi.",
            date = "Sabtu, 17 Mei 2025 | 08.00 – 12.00 WIB",
            location = "Aula Utama Gedung Rektorat Lt. 3",
            quota = 200,
            speaker = "Dr. Arief Budiman, PhD (AI Researcher – Google DeepMind)",
            category = "Teknologi",
            accentColor = Color(0xFF6750A4)
        ),
        SeminarInfo(
            title = "UI/UX Design for Mobile Apps",
            shortDescription = "Workshop intensif merancang antarmuka aplikasi mobile yang menarik & intuitif.",
            fullDescription = "Workshop hands-on ini membahas prinsip desain Material Design 3, wireframing dengan Figma, user research, serta usability testing. Peserta akan langsung mempraktikkan desain UI untuk aplikasi Android dan iOS.",
            date = "Minggu, 25 Mei 2025 | 09.00 – 15.00 WIB",
            location = "Lab Komputer Fakultas Teknik Gedung B",
            quota = 80,
            speaker = "Rina Kusuma (Lead Designer – Tokopedia)",
            category = "Desain",
            accentColor = Color(0xFF0077B6)
        ),
        SeminarInfo(
            title = "Cybersecurity Awareness Bootcamp",
            shortDescription = "Pelajari ancaman siber terkini dan cara melindungi diri di era digital.",
            fullDescription = "Bootcamp ini membahas topik keamanan siber mulai dari social engineering, phishing, SQL injection, hingga cara membangun sistem yang aman. Peserta akan mendapatkan simulasi skenario serangan dunia nyata dan strategi mitigasinya.",
            date = "Sabtu, 7 Juni 2025 | 08.30 – 16.00 WIB",
            location = "Ruang Seminar Gedung Informatika Lt. 2",
            quota = 120,
            speaker = "Budi Santoso, CEH (Cybersecurity Analyst – BSSN)",
            category = "Keamanan",
            accentColor = Color(0xFFD00000)
        ),
        SeminarInfo(
            title = "Data Science for Beginners",
            shortDescription = "Mulai perjalanan data science-mu: Python, statistik, dan visualisasi data.",
            fullDescription = "Seminar ini dirancang untuk mahasiswa yang baru mengenal data science. Materi mencakup dasar Python, pandas, matplotlib, analisis statistik sederhana, serta pengenalan machine learning menggunakan scikit-learn. Tidak diperlukan pengalaman sebelumnya.",
            date = "Minggu, 15 Juni 2025 | 09.00 – 13.00 WIB",
            location = "Auditorium Fakultas MIPA",
            quota = 150,
            speaker = "Siti Rahmawati, M.Sc (Data Scientist – Gojek)",
            category = "Data & Analytics",
            accentColor = Color(0xFF2A9D8F)
        ),
        SeminarInfo(
            title = "Startup & Digital Business Talk",
            shortDescription = "Kisah sukses founder startup lokal dan cara membangun bisnis digital dari nol.",
            fullDescription = "Seminar inspiratif yang menghadirkan para founder startup sukses untuk berbagi pengalaman membangun bisnis digital. Topik meliputi validasi ide, mencari co-founder, pitching ke investor, hingga strategi monetisasi produk digital di pasar Indonesia.",
            date = "Sabtu, 28 Juni 2025 | 13.00 – 17.00 WIB",
            location = "Amphitheater Kampus Utama",
            quota = 300,
            speaker = "Panel: 4 Founder Startup (Edtech, Fintech, Healthtech, Agritech)",
            category = "Bisnis",
            accentColor = Color(0xFFF4A261)
        )
    )

    /** Map dari title → SeminarInfo untuk lookup cepat */
    val SEMINAR_BY_TITLE: Map<String, SeminarInfo> =
        SEMINARS.associateBy { it.title }
}
