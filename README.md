# 📚 Kelasin - Aplikasi E-Learning Management

<div align="center">

![Kelasin Logo](https://img.shields.io/badge/Kelasin-E--Learning-blue?style=for-the-badge)
![Android](https://img.shields.io/badge/Android-21%2B-green?style=for-the-badge&logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose)

**Aplikasi manajemen pembelajaran untuk mahasiswa dan dosen**

[Fitur](#-fitur) • [Teknologi](#-teknologi) • [Instalasi](#-instalasi) • [Screenshots](#-screenshots)

</div>

---

## 📖 Tentang Project

**Kelasin** adalah aplikasi Android modern untuk manajemen e-learning yang memudahkan mahasiswa dan dosen dalam mengelola mata kuliah, tugas, dan catatan pembelajaran. Dibangun dengan teknologi terkini menggunakan **Kotlin** dan **Jetpack Compose**.

Project ini dibuat sebagai bagian dari tugas **Week 5 - Complete Android Studio Project** dengan mengimplementasikan seluruh komponen UI yang dipelajari.

---

## ✨ Fitur Utama

### 🎯 Fitur Umum
- ✅ **Autentikasi Lengkap** - Login & Register dengan validasi real-time
- 📚 **Manajemen Mata Kuliah** - Tambah, edit, dan hapus mata kuliah
- 📝 **Manajemen Tugas** - Tracking tugas dengan deadline dan status
- 📓 **Catatan Digital** - Buat dan kelola catatan pembelajaran
- 🌓 **Dark Mode** - Dukungan tema gelap dan terang
- 🔄 **Sync Cloud** - Sinkronisasi data dengan Supabase

### 🎓 Fitur untuk Mahasiswa
- Dashboard tugas yang harus dikerjakan
- Filter tugas berdasarkan status (Pending, Dikerjakan, Selesai)
- Notifikasi deadline tugas
- Manajemen catatan per mata kuliah

### 👨‍🏫 Fitur untuk Dosen
- Upload dan kelola tugas untuk mahasiswa
- Monitoring progress tugas mahasiswa
- Manajemen materi mata kuliah

---

## 🎨 Implementasi Week 5 Requirements

Project ini memenuhi **SEMUA** requirement tugas Week 5:

### ✅ 01. Complete Form dengan TextInputLayout
- Form registrasi lengkap dengan:
  - **Nama Lengkap** - dengan validasi minimal 2 karakter
  - **Username** - validasi minimal 3 karakter, tanpa spasi
  - **Email** - dengan validasi format email
  - **Password** - minimal 8 karakter dengan password strength indicator
  - **Konfirmasi Password** - validasi kecocokan password
- Semua field menggunakan Material Design 3 `OutlinedTextField` dengan icon dan styling glass-morphism

### ✅ 02. Advanced Validation
- **Validasi Tidak Kosong** - Semua field wajib diisi
- **Format Email** - Menggunakan `Patterns.EMAIL_ADDRESS.matcher()`
- **Password Match** - Validasi password dan confirm password identik
- **Real-time Validation** - Error muncul langsung saat user mengetik dengan `onValueChange`
- **Bonus Features:**
  - Password strength indicator visual (Lemah/OK/Kuat)
  - Username auto-remove spasi
  - Email auto-trim whitespace

### ✅ 03. Selection Controls
- **RadioGroup** - Pilihan Jenis Kelamin (Laki-laki/Perempuan)
- **Checkbox** - Pilihan Hobi dengan minimal 3 harus dipilih
- Validasi checkbox minimal 3 pilihan aktif
- Visual feedback untuk setiap pilihan

### ✅ 04. Spinner & Dialog
- **Spinner (ExposedDropdownMenu)** - Pilihan Program Studi:
  - Informatika
  - Sistem Informasi
  - Teknik Elektro
  - Teknik Industri
  - Arsitektur
- **AlertDialog** - Konfirmasi pendaftaran dengan preview data sebelum submit

### ✅ 05. Gesture Interaction (Long Press)
- Implementasi **Long Press** pada button "Refresh" di HomeScreen
- Menggunakan `pointerInput` + `detectTapGestures` dengan `onLongPress`
- Long press memunculkan dialog dengan opsi tambahan

### ✅ 06. GitHub Repository
- ✅ Project di-upload ke GitHub
- ✅ README.md lengkap dengan dokumentasi
- ✅ Proper `.gitignore` untuk Android project

---

## 🛠️ Teknologi

### Core Technologies
- **Kotlin** - Bahasa pemrograman utama
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - Design system
- **Coroutines & Flow** - Asynchronous programming

### Architecture & Libraries
- **MVVM Architecture** - Separation of concerns
- **Room Database** - Local data persistence
- **Supabase** - Backend & cloud sync
- **Ktor Client** - HTTP networking
- **Kotlinx Serialization** - JSON parsing
- **Coil** - Image loading
- **DataStore** - Preferences storage

### UI Components
- Jetpack Compose UI
- Material 3 Components
- Custom Glass-morphism design
- Animated transitions
- Custom gestures (Long press, swipe)

---

## 📱 Instalasi

### Prerequisites
- **Android Studio** Iguana | 2023.2.1 atau lebih baru
- **JDK** 17 atau lebih baru
- **Android SDK** 21+ (minimum) / 35 (target)

### Langkah Instalasi

1. **Clone Repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/kelasin.git
   cd kelasin
   ```

2. **Konfigurasi Supabase** (Opsional - untuk fitur cloud sync)
   
   Buat file `gradle.properties` di root project:
   ```properties
   SUPABASE_URL=your_supabase_url
   SUPABASE_PUBLISHABLE_KEY=your_publishable_key
   ```

3. **Build Project**
   ```bash
   ./gradlew build
   ```

4. **Run di Emulator/Device**
   - Buka project di Android Studio
   - Klik Run ▶️ atau tekan `Shift + F10`

---

## 📸 Screenshots

> **Note:** Tambahkan screenshot aplikasi di folder `/screenshots` dan update bagian ini

### Login & Register
| Login Screen | Register Form | Validation |
|-------------|---------------|------------|
| [Add Screenshot] | [Add Screenshot] | [Add Screenshot] |

### Dashboard & Features
| Dashboard | Mata Kuliah | Tugas |
|-----------|-------------|-------|
| [Add Screenshot] | [Add Screenshot] | [Add Screenshot] |

### Selection Controls & Dialog
| RadioGroup & Checkbox | Spinner | AlertDialog |
|----------------------|---------|-------------|
| [Add Screenshot] | [Add Screenshot] | [Add Screenshot] |

---

## 📂 Struktur Project

```
kelasin/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/kelasin/app/
│   │   │   │   ├── data/           # Data layer
│   │   │   │   │   ├── entity/     # Room entities
│   │   │   │   │   ├── repository/ # Repositories
│   │   │   │   │   └── supabase/   # Supabase client
│   │   │   │   ├── ui/             # UI layer
│   │   │   │   │   ├── auth/       # Login & Register screens
│   │   │   │   │   ├── main/       # Home, Tugas, Catatan screens
│   │   │   │   │   ├── detail/     # Detail screens
│   │   │   │   │   └── theme/      # Theme & styling
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                # Resources
│   │   └── test/                   # Unit tests
│   └── build.gradle.kts
├── gradle/
├── .gitignore
├── README.md
└── settings.gradle.kts
```

---

## 🎯 Fitur Week 5 Implementation Details

### 1. RegisterScreen.kt
**Lokasi:** `app/src/main/java/com/kelasin/app/ui/auth/RegisterScreen.kt`

Implementasi lengkap form registrasi dengan:
- ✅ TextInputLayout fields (line 240-361)
- ✅ Real-time validation (line 240-361)
- ✅ RadioGroup jenis kelamin (line 367-382)
- ✅ Checkbox hobi minimal 3 (line 385-410)
- ✅ Spinner program studi (line 413-449)
- ✅ AlertDialog konfirmasi (line 523-542)
- ✅ Password strength indicator (line 287-332)

### 2. HomeScreen.kt
**Lokasi:** `app/src/main/java/com/kelasin/app/ui/main/HomeScreen.kt`

Implementasi gesture interaction:
- ✅ Long press pada refresh button (line 360-368)
- ✅ Dialog action pada long press
- ✅ Visual feedback

---

## 🔐 Security Notes

⚠️ **PENTING:** File `gradle.properties` berisi API keys dan tidak di-commit ke repository. 

Untuk production:
- Gunakan environment variables
- Encrypt sensitive data
- Implement ProGuard untuk obfuscation

---

## 🚀 Future Improvements

- [ ] Notifikasi push untuk deadline tugas
- [ ] Integrasi Google Calendar
- [ ] Export catatan ke PDF
- [ ] Offline-first architecture dengan sync
- [ ] Support multi-language (i18n)
- [ ] Widget home screen
- [ ] Biometric authentication

---

## 👨‍💻 Developer

**Nama:** [Nama Anda]  
**NIM:** [NIM Anda]  
**Kelas:** [Kelas Anda]  
**Universitas:** [Nama Universitas]

**Tugas:** Week 5 - Complete Android Studio Project (E-Learning)  
**Mata Kuliah:** Pemrograman Mobile / Android

---

## 📄 License

This project is created for educational purposes as part of university coursework.

---

## 🙏 Acknowledgments

- **Material Design 3** - UI/UX guidelines
- **Jetpack Compose** - Modern Android UI
- **Supabase** - Backend as a Service
- **Android Developer Docs** - Comprehensive documentation
- Dosen pengampu untuk guidance dan requirements

---

<div align="center">

**⭐ Jika project ini membantu, berikan star!**

Made with ❤️ using Kotlin & Jetpack Compose

</div>
