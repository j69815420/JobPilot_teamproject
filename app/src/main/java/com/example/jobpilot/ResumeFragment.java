package com.example.jobpilot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumeFragment extends Fragment {

    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private RecyclerView rvResumeList;
    private ResumeAdapter adapter;
    private List<ResumeItem> resumeItems;
    private Button ivAddIcon;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Uri selectedFileUri;

    public ResumeFragment() {
    }

    // Firestore에 저장
    private void saveResumeToFirestore(ResumeItem item) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        // 문서 ID를 고유하게 (날짜 기반)
        String docId = db.collection("resumes").document(uid).collection("items").document().getId();
        item.setDocumentId(docId);
        db.collection("resumes")
                .document(uid)
                .collection("items")
                .document(docId)
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    // 저장 성공 시 로그나 토스트 표시해도 됨
                    // Toast.makeText(getActivity(), "저장 완료", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_resume, container, false);


        rvResumeList = view.findViewById(R.id.rvResumeList);
        ivAddIcon = view.findViewById(R.id.ivAddIcon);

        resumeItems = new ArrayList<>();
        adapter = new ResumeAdapter(getActivity(), resumeItems);

        rvResumeList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvResumeList.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // RecyclerView 아이템 클릭 → ResumeFeedbackActivity로 이동
        adapter.setOnItemClickListener(item -> {
            ResumeFeedbackActivity.start(requireContext(), item, item.getDocumentId());
        });


        // 플러스 버튼 클릭 → 팝업 호출
        ivAddIcon.setOnClickListener(v -> showAddResumePopup());

        // Firestore에서 기존 자기소개서 불러오기
        loadResumesFromFirestore();

        return view;
    }

    private void showAddResumePopup() {
        View popupView = getLayoutInflater().inflate(R.layout.popup_add_resume, null);
        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(true);
        popupWindow.showAtLocation(getView(), Gravity.CENTER, 0, 0);

        // 직접쓰기 버튼
        Button btnDirectWrite = popupView.findViewById(R.id.btnDirectWrite);
        btnDirectWrite.setOnClickListener(v -> {
            popupWindow.dismiss();

            ResumeWriteFragment writeFragment = new ResumeWriteFragment();
            writeFragment.setOnResumeWriteCompleteListener(item -> {
                resumeItems.add(item);
                adapter.notifyItemInserted(resumeItems.size() - 1);
                rvResumeList.scrollToPosition(resumeItems.size() - 1);

                // Firestore 저장
                saveResumeToFirestore(item);
            });

            getParentFragmentManager().beginTransaction()
                    .add(R.id.container_main, writeFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 파일 추가 버튼
        Button btnAddFile = popupView.findViewById(R.id.btnAddFile);
        btnAddFile.setOnClickListener(v -> {
            popupWindow.dismiss();
            openFilePicker();
        });
    }

    // 파일 선택
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {
                "text/plain",
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "파일 선택"), PICK_FILE_REQUEST_CODE);
    }

    // 파일 읽기
    private String readTextFromUri(Uri uri) {
        String fileName = getFileName(uri);

        if (fileName != null) {
            if (fileName.endsWith(".pdf")) return readPdfFromUri(uri);
            else if (fileName.endsWith(".docx")) return readDocxFromUri(uri);
        }
        return readTxtFromUri(uri);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            android.database.Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private String readTxtFromUri(Uri uri) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) stringBuilder.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
            return "파일을 읽을 수 없습니다.";
        }
        return stringBuilder.toString();
    }

    private String readPdfFromUri(Uri uri) {
        PDDocument document = null;
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return "PDF 파일을 열 수 없습니다.";
            }

            // PDFBox 로그 관련 오류 방지
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

            document = PDDocument.load(inputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            return text != null && !text.trim().isEmpty() ? text : "PDF에서 텍스트를 추출할 수 없습니다.";

        } catch (Throwable e) { // Exception → Throwable 로 변경 (PDFBox 내부 오류까지 잡음)
            e.printStackTrace();
            android.util.Log.e("ResumeFragment", "PDF 읽기 오류", e);
            return "PDF 파일을 읽는 중 오류 발생: " + e.getMessage();

        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private String readDocxFromUri(Uri uri) {
        StringBuilder text = new StringBuilder();
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri)) {
            XWPFDocument document = new XWPFDocument(inputStream);
            for (XWPFParagraph paragraph : document.getParagraphs())
                text.append(paragraph.getText()).append("\n");
            document.close();
        } catch (IOException e) {
            e.printStackTrace();
            return "Word 파일을 읽을 수 없습니다.";
        }
        return text.toString();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            ResumeItem item = null;
            if (selectedFileUri != null) {
                String content = readTextFromUri(selectedFileUri);
                String fileName = getFileName(selectedFileUri);

                // ResumeItem 생성
                // PDF나 DOCX 내용이 너무 길면 앞부분만 요약해서 표시
                String previewText = content.length() > 600 ? content.substring(0, 600) + "..." : content;

                // 날짜 생성
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                //  ResumeItem 생성 시 title과 growth(자기소개 부분 등)에 기본 내용 넣기
                item = new ResumeItem(
                        content,          // content: 전체 내용 (상세 보기용)
                        date,             // date
                        fileName,         // title: 파일 이름
                        previewText,      // growth: 본문 미리보기
                        "", "", ""    // 나머지 필드는 비워둠
                );
                //  documentId 생성 및 할당
                String docId = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
                item.documentId = docId;

                resumeItems.add(item);
                adapter.notifyItemInserted(resumeItems.size() - 1);
                rvResumeList.scrollToPosition(resumeItems.size() - 1);

                saveResumeToFirestore(item);
            }

            // Firestore에도 저장
            saveResumeToFirestore(item);
        }
    }


    // Firestore 불러오기
    private void loadResumesFromFirestore() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        db.collection("resumes")
                .document(uid)
                .collection("items")
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    //  기존 데이터 초기화
                    resumeItems.clear();

                    //  Firestore에서 불러온 데이터 추가
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        ResumeItem item = doc.toObject(ResumeItem.class);
                        if (item != null) {
                            //  documentId가 없으면 문서 ID로 설정
                            if (item.documentId == null || item.documentId.isEmpty()) {
                                item.documentId = doc.getId();
                            }
                            resumeItems.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(Throwable::printStackTrace);

    }
}