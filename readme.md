# 内蔵ストレージに暗号化データを保持する

# 検討必要かもトピック

* KeyStoreに指定するaliasはグローバルにユニークじゃないといけないのか？

# 参考URLと読んだ感想

* https://coky-t.gitbook.io/owasp-mstg-ja/android-tesutogaido/0x05d-testing-data-storage
   * セキュリティ観点におけるandroidのデータストレージの種類の説明
   * *androidのテスト* の文脈ではあるが基本の勉強の参考になる
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
   * サンプルあるので良い
   * が、そのままだと IllegalBlockSizeException が出てうまくいかない
   * https://teratail.com/questions/122006 を参考にEncpyt時の`Cipher#init`に`spec`を指定するとうまくいった
      * Decrypt時には不要だったが、理由は理解しきれていない

# アプリの外観(ソースコード)

* 暗号化、複合化の処理と、暗号化データのSharedPreferenceへの保存
   * 保存するデータはkey:"User"として1つだけ保存
* 各データはSharedPreferenceへの保存を見越してBase64エンコードしている
   * 公式のサンプルもそうなってる
* 認証キーを生成
   * MainActivity.java#createNewKey
* キーを使って暗号化
   * MainActivity.java#encryptString
* キーを使って複合化
   * MainActivity.java#decryptString
* 暗号化データの保存(SharedPreference)
   * MainActivity.java#onSaveButtonClicked
* 保存データの読み出し(SharedPreference)
   * MainActivity.java#onLoadButtonClicked
* 保存データの破棄(SharedPreference)
   * MainActivity.java#onPurgeButtonClicked
* 起動時に保存データがあるかどうか確認しつつ複合化
   * MainActivity.java#loadSavedData

# アプリの外観(見た目)

* [ENCRYPT]ボタン
   * テキストボックス内の文字列を暗号化して結果を表示
* [DECRYPT]ボタン
   * テキストボックス内の文字列を複合化して結果を表示(encryptとは別窓)
* [CLEAR]ボタン
   * 暗号化/複合化データの初期化
* [SAVE]ボタン
   * **[ENCRYT]ボタンを押してから使います**
   * 暗号化データをSharedPreferenceに保存
* [LOAD]ボタン
   * SharedPreferenceに保存しているデータの読み出し
* [PURGE]ボタン
   * SharedPreferenceに保存しているデータの破棄
