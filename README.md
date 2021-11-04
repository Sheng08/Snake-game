# Snake-game

### 作品介紹
此為單人作品，利用Java語言完成多人遊玩的貪食蛇遊戲，並以Client-Server架構建立遊戲平台，利用ThreadPool的方式設計Server端，提供多人連線時有效保持伺服器校能，而Client端與Server端之間訊息的傳輸則使用NIO方式進行溝通，並利用物件導向與資料結構技術以及相關套件建構出遊戲核心程式。詳細說明連結：https://reurl.cc/bn90Ro

目前local端實現多人貪食蛇遊戲

###  本作品中，我撰寫了以下主要功能：
>1.	前端玩家與後端伺服器，皆由Java語言完成
>2.	利用JFrame框架建立遊戲畫面
>3.	以ThreadPool設計伺服器，可使伺服器接受多位玩家並減少overhead的成本
>4.	以NIO建立Client-Server間溝通方式，以廣播方式同步各玩家遊戲狀態，如：分數、位置
>5.	實現隨機生成目標、加速移動、分數排名、可大量玩家遊玩等遊戲特色
>6.	將成果打包為.jar檔，方便不同玩家電腦遊玩

###  成果截圖：
遊戲進行畫面<br>
![1](https://user-images.githubusercontent.com/58781800/140345117-b65f0959-8a58-466e-9024-e8e7d3034bc8.png)

多位玩家同時遊玩與狀態同步<br>
![2](https://user-images.githubusercontent.com/58781800/140345358-a611eedd-08eb-462f-8e9d-8e971b6de789.png)
