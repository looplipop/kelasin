<div align="center">

![Kelasin Logo](https://img.shields.io/badge/Kelasin-E--Learning-blue?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose)
![Supabase](https://img.shields.io/badge/Supabase-Database-3ECF8E?style=for-the-badge&logo=supabase)

# 📚 Kelasin - Modern E-Learning Management

**Aplikasi manajemen pembelajaran terpadu untuk ekosistem digital cerdas mahasiswa dan dosen.**

[Fitur](#-fitur-unggulan) • [Arsitektur & Alur](#-arsitektur--alur-sistem) • [Database](#-skema-database) • [Screenshots](#-galeri-tangkapan-layar) • [Download](#-unduh-rilis)

</div>

---

## 📖 Tentang Kelasin

**Kelasin** adalah aplikasi Android berbasis *Jetpack Compose* yang mendigitalkan seluruh proses perkuliahan dan administrasi kampus. Mulai dari manajemen **Mata Kuliah**, **Tugas**, **Absensi**, hingga fitur terbaru yaitu **Pendaftaran Seminar**.

Ditenagai oleh **Supabase (PostgreSQL)**, aplikasi ini menyajikan data secara *real-time*, sinkronisasi antar modul secara presisi, dan estetika UI/UX tingkat premium dengan *Glassmorphism* dan *Dynamic Gradient Colors*.

---

## ✨ Fitur Unggulan

### 🎓 1. Manajemen Akademik
- **Mata Kuliah & Absensi**: Mengelola jadwal kuliah, ruangan, beserta catatan kehadiran mahasiswa yang tersinkronisasi.
- **Tugas & Materi**: Distribusi *file* materi PDF dan pelacakan *deadline* tugas secara responsif.
- **Catatan Digital & Chat**: Ruang diskusi kolaboratif.

### 🎫 2. Modul Seminar (Fitur Terbaru!)
- **Dynamic Gradient UI**: Kartu seminar dan layar riwayat mengadopsi palet warna *custom* yang ditentukan dari backend, menciptakan gradien warna cantik yang disesuaikan per kategori seminar.
- **Pendaftaran One-Tap**: Mendaftar seminar lengkap dengan validasi kuota secara *real-time*.
- **Pertanyaan Dinamis**: Form registrasi dapat dikonfigurasi admin untuk menambah pertanyaan pilihan ganda (*Dropdown*) atau isian secara *on-the-fly*.
- **Sistem Status Tiket**: Sinkronisasi status "Terdaftar", "Lolos", atau "Tidak Lolos" yang otomatis diperbarui di layar *Riwayat*.

### 🔒 3. Keamanan & Validasi
- **Real-time Error Handling**: Mengoreksi format *email*, kekuatan *password*, dan form pendaftaran seketika saat user mengetik (tanpa menunggu tombol *Submit*).
- **Konfirmasi Persetujuan**: Menggunakan *AlertDialog* dan *Checkbox Consent* untuk memastikan tidak ada kesalahan input.

---

## 🏗 Arsitektur & Alur Sistem

### Alur Aplikasi (Use Case)

```mermaid
usecaseDiagram
    actor Mahasiswa as "👨‍🎓 Mahasiswa"
    actor Admin as "👨‍💼 Admin/Dosen"
    
    package "Aplikasi Kelasin" {
        usecase "Login & Register" as UC1
        usecase "Manajemen Mata Kuliah" as UC2
        usecase "Upload & Unduh Materi" as UC3
        usecase "Melakukan Absensi" as UC4
        usecase "Manajemen Tugas" as UC5
        usecase "Daftar Seminar" as UC6
        usecase "Kelola Kuota & Seminar" as UC7
    }
    
    Mahasiswa --> UC1
    Mahasiswa --> UC3
    Mahasiswa --> UC4
    Mahasiswa --> UC5
    Mahasiswa --> UC6
    
    Admin --> UC1
    Admin --> UC2
    Admin --> UC3
    Admin --> UC7
```

### Flow Sinkronisasi Data (Seminar)

```mermaid
sequenceDiagram
    participant User as Mahasiswa (UI)
    participant Repo as SeminarRepository
    participant DB as Supabase PostgreSQL
    
    User->>Repo: Buka Halaman "Riwayat Saya"
    Repo->>DB: Fetch Seminar Registrations (JOIN seminars)
    DB-->>Repo: Return Data + color_hex & is_selection_enabled
    Repo-->>User: Render Gradient Card & Status Label "Terdaftar"
```

---

## 🗄 Skema Database

Kelasin menggunakan model relasional yang efisien di backend **Supabase**. Berikut adalah cuplikan entitas utama:

```mermaid
erDiagram
    USERS ||--o{ MAHASISWA : "memiliki profil"
    USERS ||--o{ MATA_KULIAH : "mengelola"
    MATA_KULIAH ||--o{ MATERI : "menyimpan"
    MATA_KULIAH ||--o{ TUGAS : "memberikan"
    MATA_KULIAH ||--o{ ABSENSI : "mencatat"
    USERS ||--o{ SEMINAR_REGISTRATIONS : "mendaftar"
    SEMINARS ||--o{ SEMINAR_REGISTRATIONS : "menerima tiket"

    SEMINARS {
        uuid id PK
        string title
        int quota
        string category
        varchar color_hex
        bool is_selection_enabled
    }

    SEMINAR_REGISTRATIONS {
        uuid id PK
        uuid user_id FK
        uuid seminar_id FK
        varchar selection_status
    }
```

---

## 📸 Galeri Tangkapan Layar

### Autentikasi & Dashboard Utama
| Login & Form Role | Home Dashboard | Dark Mode |
|:---:|:---:|:---:|
| <img src="docs/images/form-login.jpg" width="250"> | <img src="docs/images/dashboard-home.jpg" width="250"> | <img src="docs/images/dark-mode.jpg" width="250"> |
| <img src="docs/images/form-awal-pemilihan-role-mahasiswa-admin.jpg" width="250"> |

### Menu Akademik Lengkap
| Kalender Akademik | Mata Kuliah | Absensi & PDF |
|:---:|:---:|:---:|
| <img src="docs/images/menu-kalender-akademik.jpg" width="250"> | <img src="docs/images/menu-matakuliah.jpg" width="250"> | <img src="docs/images/menu-absensi.jpg" width="250"> |
| <img src="docs/images/lihat-absensi.jpg" width="250"> | <img src="docs/images/absensi-pdf.jpg" width="250"> |

| Tugas & Materi | Catatan & Chat |
|:---:|:---:|
| <img src="docs/images/menu-tugas.jpg" width="250"> | <img src="docs/images/menu-materi.jpg" width="250"> |
| <img src="docs/images/menu-catatan.jpg" width="250"> | <img src="docs/images/catatan-chat.jpg" width="250"> |

### 🚀 Eksklusif: Seminar Registration App
| Katalog Seminar | Form Pendaftaran Lengkap | Tiket & Detail Peserta |
|:---:|:---:|:---:|
| <img src="docs/images/1.png" width="250"> | <img src="docs/images/seminar.jpg" width="250"> | <img src="docs/images/2.png" width="250"> |
| <img src="docs/images/3.png" width="250"> | <img src="docs/images/4.png" width="250"> | <img src="docs/images/5.png" width="250"> |
| <img src="docs/images/6.png" width="250"> | <img src="docs/images/7.png" width="250"> | <img src="docs/images/8.png" width="250"> |

---

## 🛠 Teknologi yang Digunakan

- **Frontend:** Kotlin, Jetpack Compose (Material 3), Compose Navigation, Coil (Image Loading).
- **Backend:** Supabase (Auth, Postgres Database, Real-time).
- **Networking:** Ktor Client, Kotlinx Serialization.
- **Arsitektur:** MVVM (Model-View-ViewModel), Repository Pattern.

---

## 📦 Unduh Rilis

Aplikasi siap pakai (APK) telah dikompilasi dan dapat diunduh pada halaman **Releases** repositori ini.

👉 **[Download Kelasin APK Latest Release](https://github.com/looplipop/kelasin/releases)**

---
<div align="center">
Dibuat dengan ❤️ untuk kemudahan pendidikan digital.
</div>
