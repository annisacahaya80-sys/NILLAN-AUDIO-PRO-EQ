# NILLAN AUDIO PRO EQ — Source V2 (Parametric EQ enabled)

Proyek sumber Android native untuk Oppo A54 / Android 11. Ini **bukan APK siap pasang**; APK dibangun melalui GitHub Actions.

## Yang ada pada source ini
- Graphic EQ 10-band / 21-band.
- Parametric EQ 5-band dengan kontrol frekuensi pusat (20 Hz–20 kHz), gain (-12 sampai +12 dB), dan Q (0,3–10).
- Parameter PEQ aktif secara default, tersimpan pada setelan dan preset JSON, serta dapat diaktif/nonaktifkan.
- Implementasi PEQ saat ini memetakan kurva bell ke band EQ pada `DynamicsProcessing` Android. Karena API system EQ tidak menyediakan biquad dengan Q bebas, ini adalah **pendekatan berdasarkan band yang tersedia**, bukan filter biquad mandiri beresolusi penuh.
- Bass Boost, gain, compressor multiband, limiter, preset pabrik dan preset pengguna.

## Catatan penting kompatibilitas
Aplikasi membuat efek audio Android pada audio session 0. Android/OEM dapat menolak atau tidak menerapkan efek itu ke audio aplikasi lain. Karena itu pemrosesan musik Brave di Oppo A54 harus diuji pada perangkat nyata. Source berhasil dibangun belum berarti efek global Brave telah terbukti berfungsi.

## Membangun APK dengan GitHub Actions
1. Buat repository GitHub baru.
2. Unggah seluruh isi folder proyek ini ke root repository (folder `.github/workflows` harus ikut diunggah).
3. Buka tab **Actions** dan jalankan workflow **Build NILLAN AUDIO PRO EQ APK**.
4. Jika workflow sukses, unduh artifact `NILLAN-AUDIO-PRO-EQ-debug-apk`.
5. Ekstrak artifact dan pasang `app-debug.apk` pada Oppo A54. Izinkan pemasangan dari sumber tersebut bila Android meminta.

Workflow menggunakan Gradle 8.9 dan JDK 17. Proyek memerlukan koneksi internet saat GitHub Actions mengunduh Gradle/Android build dependencies.
