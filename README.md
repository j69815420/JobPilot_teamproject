# JobPilot
<img width="1000" height="900" alt="Image" src="https://github.com/user-attachments/assets/c15d8840-5584-4c9a-a2a9-2e0f86f5c1d0" />


## 📱프로젝트 소개
취업을 준비하는 사용자가 실전 면접처럼 연습하고 실시간으로 AI로부터 구체적인 피드백을 제공받을 수 있는 안드로이드 앱을 구현한 팀 프로젝트.
### 개발 기간
- 25.03~25.11 (8개월)
 
### 개발 인원
- 4명 (디자인/프론트엔드/백엔드/AI)
## 🔎주요 기능
- 사용자의 음성 및 텍스트 답변을 분석하여 AI 기반의 평가 점수 및 피드백을 제공
- AI 생성 질문 또는 저장된 카테고리별 면접 질문 제공
- AI가 생성한 평가 점수 및 피드백 결과 조회 기능
- 자기소개서 작성 및 파일 첨부 후 AI 피드백 제공
### 👩‍💻담당한 역할 및 구현 기능
- 사용자 계정 관리 기능 구현 (회원가입, 로그인, 정보수정, 회원탈퇴)
- 카테고리별 면접 질문 데이터를 저장 및 질문 제공 기능 구현
- AI 평가 점수 및 피드백 결과 저장 및 항목별 조회 기능 구현
- 자기소개서 저장 · 수정 · 삭제 기능 구현
## ✏기술 스택
<img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/python-EE4B4B?style=for-the-badge&logo=python&logoColor=white"> <img src="https://img.shields.io/badge/FLASK-grey?style=for-the-badge&logo=flask&logoColor=white"/> <img src="https://img.shields.io/badge/OpenAI-6B4ECC?style=for-the-badge&logo=openai&logoColor=white"/> <img src="https://img.shields.io/badge/FIGMA-D2432A?style=for-the-badge&logo=figma&logoColor=white"/><br> <img src="https://img.shields.io/badge/FIREBASE-yellow?style=for-the-badge&logo=firebase&logoColor=white"/> <img src="https://img.shields.io/badge/ANDROID-GREEN?style=for-the-badge&logo=android&logoColor=white"/> <img src="https://img.shields.io/badge/GITHUB-black?style=for-the-badge&logo=github&logoColor=white"/>
## ⚙시스템 구조
<img width="600" height="400" alt="Image" src="https://github.com/user-attachments/assets/aa5d243e-e2dd-4c3c-89b8-2d8b78ca10cc" />

## 🛠트러블 슈팅
|상황|피드백 조회 화면에서 실전모드 피드백과 점수를 조회할 수 있도록 구현하는 과정에서 DB에 저장된 결과데이터를 기존 실전모드 결과 화면 UI와 동일한 형태로  재사용하여 실전모드가 끝난 직후 화면과 동일하게 제공하고자 하였습니다. 날짜별로 묶인 항목을 클릭하면 해당 결과와 피드백을 확인할 수 있도록 구현하고자 하였으나 항목 클릭 시 점수만 표시되고 피드백 내용이 출력되지 않는 문제가 발생하였습니다.|


|원인|실전모드 실행 직후에는 음성 변환과 GPT 호출로 데이터가 생성되는 흐름을 통해 화면에 표시되는 실행 구조를 피드백 조회에서도 그대로 사용하려다 보니 몇 데이터 값이 비워 있어 결과적으로 점수만 화면에 표시되는 현상이 발생하게 되었습니다.|  


|해결|피드백 조회 전용 Activity를 새로 생성하여 DB에서 불러온 점수와 각 피드백 데이터를 Intent로 전달 받아 동일한 UI에 출력하도록 구현함으로써 문제를 해결하였습니다.|





