# 内蔵ストレージに暗号化データを保持する

# 参考URLと読んだ感想

* https://developer.android.com/training/articles/security-tips
   * 基本は「内部ストレージ」で良い（様にandroidは設計されている)
   * コンテンツプロバイダ := 他のアプリと共有する仕組み
   * それ以上はmanufest上の権限の議論が多い
* https://developer.android.com/topic/security/best-practices
   * 内容自体は参考になるが、直接的な暗号化についての話は無い
* https://developer.android.com/topic/security/data
   * Securityライブラリは**アルファ版**らしい
      * とりあえずやるだけやってみる。
      * 別の手段があるならそれを使うようにする
   * EncryptFile
      * 暗号化したファイルらしい
      * 実際に使ってみて中身がどんなファイルなのか？見てみてもいいかも
      * 今回はuser/passを保存するだけなのでスキップ
   * EncryptedSharedPreferences
      * それなりによさそうには見えるがコードが断片すぎて使えない
      * https://qiita.com/rmorimot/items/ba9e79825bccaa9c0abe
         * やはり簡単そう。。。だが**alpha版**だと。。。
      * やはり、、keystoreのようだ。。
* https://developer.android.com/training/articles/keystore
   * 各アルゴリズムのサポート状況を見るに **Android API23以上(android 7以上)** あれば十分使えそうに見える
   * Android Keystore プロバイダ
      * > 個々のアプリで、そのアプリだけがアクセスできる独自の認証情報を保存できるようにする場合は、Android Keystore プロバイダを使用します
      * とあるのでこれがよさそう
   * 説明はわかる。けど「で、なになん？」が薄い。。。
   * ので、↓のURLを参考にした
* https://qiita.com/f_nishio/items/485490dea126dbbb5001

# 使い方

## Jetpack::Security

1. jetpackライブラリを有効にする
   * https://developer.android.com/jetpack/docs/getting-started 
