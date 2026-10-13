NILLAN AUDIO DIGITAL MIXER V2 — PANDUAN REALISTIS UNTUK OPPO A54
Tanggal: 14 Oktober 2026

TUJUAN
Memproses musik YouTube di HP 1 menggunakan equalizer sistem Android, lalu mengirim output analog ke sound card V8. Mikrofon dinamis masuk ke V8. Output V8 diteruskan ke X50, lalu ke HP 2 untuk TikTok Live.

PENTING: ZIP NILLAN_AUDIO_MIXER_V2 yang lama berisi index.html dan README saja. Itu adalah antarmuka web/demo, bukan APK Android dan tidak dapat mengubah audio YouTube dengan sendirinya.

ARSITEKTUR YANG DISARANKAN
1. HP 1 (Oppo A54, Android 11): YouTube + aplikasi equalizer sistem.
2. Output headphone 3,5 mm HP 1 -> kabel AUX 3,5 mm stereo (TRS) -> input MUSIC/ACCOMPANIMENT pada V8.
3. Mikrofon dinamis -> input MIC pada V8, menggunakan konektor yang sesuai dengan port V8.
4. Output audio V8 -> input LINE/MUSIC yang sesuai pada X50. Cocokkan port berdasarkan tulisan pada perangkat; jangan sambungkan output speaker berdaya tinggi ke input sound card.
5. Output live X50 -> kabel live yang sesuai -> HP 2.

PENGUJIAN EQ YOUTUBE
Aplikasi yang dapat dicoba: Poweramp Equalizer resmi dari Google Play.
Tautan: https://play.google.com/store/apps/details?id=com.maxmpz.equalizer

Langkah:
1. Pasang Poweramp Equalizer dari Google Play.
2. Buka aplikasi dan izinkan akses yang diminta hanya jika Anda memahami kegunaannya.
3. Cari pengaturan Global Equalization jika tersedia pada versi aplikasi Anda, lalu aktifkan.
4. Jika ada Advanced Player Tracking, ikuti panduan resmi aplikasi. Fitur tertentu dapat memerlukan izin tambahan melalui ADB; jangan anggap Shizuku otomatis menggantikan izin itu.
5. Putar video YouTube. Atur Preamp sementara ke -6 dB dan ubah satu band EQ dengan jelas (misalnya 1 kHz turun banyak) untuk menguji apakah suara benar-benar berubah.
6. Hubungkan kabel HP 1 ke input musik V8 dan dengarkan melalui sistem live. Jangan menaikkan gain tinggi saat pengujian.
7. Jika EQ tidak mengubah suara YouTube, hentikan konfigurasi di situ: kemungkinan jalur audio YouTube tidak terhubung ke efek EQ pada perangkat ini. Antarmuka mixer web tidak dapat mengatasi batasan itu.

CONTOH START EQ NETRAL UNTUK UJI SUARA
31 Hz: 0 dB
62 Hz: 0 dB
125 Hz: 0 dB
250 Hz: 0 dB
500 Hz: 0 dB
1 kHz: 0 dB
2 kHz: 0 dB
4 kHz: 0 dB
8 kHz: 0 dB
16 kHz: 0 dB
Preamp: -3 sampai -6 dB jika menaikkan beberapa band.
Ini hanya titik awal pengujian, bukan preset wajib. Hindari boost besar karena dapat menyebabkan clipping.

BATASAN TEKNIS ANDROID
Android 10+ menyediakan API untuk menangkap audio dari aplikasi lain, tetapi hanya bila aplikasi sumber mengizinkan penangkapan. API itu bukan jalan pintas yang pasti untuk membuat EQ sistem, dan memutar ulang hasil tangkapan bisa menyebabkan audio ganda/latensi. Karena itu, proyek tidak mengklaim memiliki pemrosesan YouTube internal yang belum dibangun dan diuji.

JIKA EQ GLOBAL TIDAK BERHASIL
Pilihan paling andal tanpa root adalah memakai sumber musik yang diputar melalui aplikasi pemutar yang didukung equalizer, atau memakai equalizer/perangkat DSP eksternal di jalur analog. Untuk syarat YouTube langsung + EQ di HP 1, dukungan harus dibuktikan lewat uji audio pada Oppo A54 Anda.

KESELAMATAN KONEKSI
- Mulai dengan volume HP, V8, dan X50 rendah.
- Jangan sambungkan output speaker amplifier mobil langsung ke V8/X50/HP.
- Pastikan port V8/X50 adalah input/output yang benar dari label perangkat.
- Jika ada dengung, coba kabel pendek dan hindari menghubungkan dua output ke satu input tanpa mixer/isolator yang sesuai.
