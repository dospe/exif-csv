# ViewModel is instantiated reflectively by the default ViewModelProvider factory.
-keep class io.github.dospe.exifcsv.MainViewModel {
    <init>(android.app.Application);
}
