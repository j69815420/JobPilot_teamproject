import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
from firebase_admin import auth
from flask import Flask, request, jsonify
from flask_cors import CORS

print("=" * 50)
print("서버 시작 중...")
print("=" * 50)

app = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})
app.config['JSON_AS_ASCII'] = False

# Firebase 초기화
cred = credentials.Certificate(r"FIREBASE_CREDENTIALS")
firebase_admin.initialize_app(cred)
db = firestore.client()
print("Firebase 초기화 완료")


@app.route('/')
def home():
    return jsonify({'status': 'ok', 'message': '서버가 정상 작동 중입니다.'})


@app.route('/api/user/delete', methods=['DELETE'])
def delete_user():
    """회원 탈퇴 API (Firebase 로그인)"""
    print("\n" + "=" * 80)
    print("🗑️ 회원 탈퇴 함수 실행!")
    
    auth_header = request.headers.get('Authorization')
    
    if not auth_header or not auth_header.startswith("Bearer "):
        return jsonify({"status": "error", "message": "로그인이 필요합니다."}), 401
    
    firebase_id_token = auth_header.split(' ')[1].strip()
    
    if not firebase_id_token:
        return jsonify({"status": "error", "message": "유효하지 않은 토큰입니다."}), 401
    
    try:
        # 토큰 검증
        decoded_token = auth.verify_id_token(firebase_id_token)
        user_uid = decoded_token['uid']
        print(f"토큰 검증 성공! UID: {user_uid}")
        
        # Firestore 사용자 정보 조회
        user_ref = db.collection('users').document(user_uid)
        user_doc = user_ref.get()
        
        user_id = None
        if user_doc.exists:
            user_data = user_doc.to_dict()
            user_id = user_data.get('id', 'Unknown')
            print(f" 사용자 정보: {user_id}")
            user_ref.delete()
            print(f"Firestore 삭제 완료")
        else:
            print(f"Firestore에 데이터 없음")
        
        # Firebase Auth 삭제
        try:
            auth.delete_user(user_uid)
            print(f" Firebase Auth 삭제 완료")
        except auth.UserNotFoundError:
            print(f" Firebase Auth에 사용자 없음")
        
        return jsonify({
            'status': 'success',
            'message': '회원탈퇴가 완료되었습니다.',
            'deleted_uid': user_uid,
            'deleted_user_id': user_id
        }), 200
        
    except Exception as e:
        print(f" 오류: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({'status': 'error', 'message': f'회원탈퇴 실패: {str(e)}'}), 500
    finally:
        print("=" * 80 + "\n")


@app.route('/api/user/delete/kakao', methods=['DELETE'])
def delete_kakao_user():
    """카카오 회원 탈퇴 API"""
    print("\n" + "=" * 80)
    print("카카오 회원 탈퇴 함수 실행!")
    
    # JSON body에서 uid 가져오기
    data = request.get_json()
    
    if not data or 'uid' not in data:
        return jsonify({"status": "error", "message": "uid가 필요합니다."}), 400
    
    kakao_uid = data['uid']
    
    if not kakao_uid:
        return jsonify({"status": "error", "message": "유효하지 않은 uid입니다."}), 400
    
    try:
        print(f" 카카오 사용자 UID: {kakao_uid}")
        
        # Firestore에서 카카오 사용자 정보 조회 및 삭제
        user_ref = db.collection('users').document(kakao_uid)
        user_doc = user_ref.get()
        
        user_id = None
        if user_doc.exists:
            user_data = user_doc.to_dict()
            user_id = user_data.get('id', 'Unknown')
            print(f" 카카오 사용자 정보: {user_id}")
            
            user_ref.delete()
            print(f" Firestore 삭제 완료")
        else:
            print(f" Firestore에 카카오 사용자 데이터 없음")
            # 데이터가 없어도 성공으로 처리 (이미 삭제됨)
        
        return jsonify({
            'status': 'success',
            'message': '회원탈퇴가 완료되었습니다.',
            'deleted_uid': kakao_uid,
            'deleted_user_id': user_id
        }), 200
        
    except Exception as e:
        print(f" 카카오 탈퇴 오류: {str(e)}")
        import traceback
        traceback.print_exc()
        return jsonify({'status': 'error', 'message': f'회원탈퇴 실패: {str(e)}'}), 500
    finally:
        print("=" * 80 + "\n")


if __name__ == '__main__':
    print("\n" + "=" * 80)
    print(" Flask 서버 시작!")
    print(" 등록된 API 엔드포인트:")
    for rule in app.url_map.iter_rules():
        methods = ', '.join(rule.methods - {'HEAD', 'OPTIONS'})
        print(f"  - {rule.rule} [{methods}]")
    print("=" * 80 + "\n")
    app.run(debug=True, use_reloader=False, host='0.0.0.0', port=5000)