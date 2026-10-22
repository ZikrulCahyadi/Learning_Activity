package com.firstresources.learningactivity.ui.evaluasi

data class EvaluationQuestion(
    val id: String,
    val number: Int,
    val text: String,
    val options: List<String>
)

data class EvaluationCategory(
    val category: String,
    val items: List<EvaluationQuestion>
)

object EvaluationConstants {
    val VALUES = listOf("A", "B", "C", "D", "E")

    val CATEGORIES = listOf(
        EvaluationCategory(
            category = "A. ISI MATERI PELATIHAN",
            items = listOf(
                EvaluationQuestion(
                    id = "q1",
                    number = 1,
                    text = "Manfaat materi training terhadap pekerjaan",
                    options = listOf(
                        "Sangat bermanfaat sekali",
                        "Bermanfaat bagi pekerjaan",
                        "Cukup bermanfaat",
                        "Kurang bermanfaat",
                        "Tidak bermanfaat"
                    )
                ),
                EvaluationQuestion(
                    id = "q2",
                    number = 2,
                    text = "Kelengkapan materi",
                    options = listOf(
                        "Sangat lengkap",
                        "Lengkap",
                        "Cukup",
                        "Kurang",
                        "Tidak dipersiapkan"
                    )
                ),
                EvaluationQuestion(
                    id = "q3",
                    number = 3,
                    text = "Lewat training ini keahlian dan ilmu saya bertambah",
                    options = listOf(
                        "Sangat setuju sekali",
                        "Setuju",
                        "Cukup setuju",
                        "Kurang setuju",
                        "Tidak setuju"
                    )
                ),
                EvaluationQuestion(
                    id = "q4",
                    number = 4,
                    text = "Materi training ini dapat saya terapkan di pekerjaan saya",
                    options = listOf(
                        "Sangat setuju sekali",
                        "Setuju",
                        "Cukup setuju",
                        "Kurang setuju",
                        "Tidak setuju"
                    )
                )
            )
        ),
        EvaluationCategory(
            category = "B. INSTRUKTUR / TRAINER",
            items = listOf(
                EvaluationQuestion(
                    id = "q5",
                    number = 5,
                    text = "Penguasaan materi oleh instruktur / trainer",
                    options = listOf(
                        "Sangat menguasai",
                        "Bagus",
                        "Sedang",
                        "Kurang",
                        "Tidak menguasai"
                    )
                ),
                EvaluationQuestion(
                    id = "q6",
                    number = 6,
                    text = "Metode penyampaian materi oleh instruktur / trainer",
                    options = listOf(
                        "Sangat mudah dipahami dan menarik",
                        "Mudah dipahami",
                        "Cukup bisa dipahami",
                        "Kurang dipahami",
                        "Sulit dipahami dan tidak menarik"
                    )
                ),
                EvaluationQuestion(
                    id = "q7",
                    number = 7,
                    text = "Kecepatan dalam pemberian materi training",
                    options = listOf(
                        "Sangat teratur ada intonasi dan mudah diikuti",
                        "Teratur ada intonasi dan mudah diikuti",
                        "Sedang bisa diikuti",
                        "Cepat kurang bisa diikuti",
                        "Sangat cepat tidak bisa diikuti"
                    )
                ),
                EvaluationQuestion(
                    id = "q8",
                    number = 8,
                    text = "Contoh-contoh yang diberikan oleh trainer",
                    options = listOf(
                        "Sesuai dan sangat lengkap",
                        "Sesuai dan cukup lengkap",
                        "Sesuai namun kurang lengkap",
                        "Kurang sesuai dengan kondisi yg ada",
                        "Tidak sesuai dengan kondisi yang ada"
                    )
                ),
                EvaluationQuestion(
                    id = "q9",
                    number = 9,
                    text = "Trainer dalam menjawab pertanyaan dan bimbingan praktek",
                    options = listOf(
                        "Jelas dan detail",
                        "Jelas dan mengenai sasaran",
                        "Cukup mengenai sasaran",
                        "Kurang mengenai sasaran",
                        "Tidak mengenai sasaran"
                    )
                )
            )
        ),
        EvaluationCategory(
            category = "C. FASILITAS DAN PELAYANAN",
            items = listOf(
                EvaluationQuestion(
                    id = "q10",
                    number = 10,
                    text = "Fasilitas kelas (White board, flip chart, LCD, kenyamanan ruangan, dsb)",
                    options = listOf(
                        "Baik dan sangat lengkap",
                        "Baik dan lengkap",
                        "Cukup lengkap",
                        "Kurang lengkap",
                        "Tidak lengkap"
                    )
                ),
                EvaluationQuestion(
                    id = "q11",
                    number = 11,
                    text = "Pelayanan Mess & Akomodasi (Makan, penginapan, dsb)",
                    options = listOf(
                        "Sangat baik",
                        "Baik",
                        "Cukup",
                        "Kurang",
                        "Tidak memadai"
                    )
                ),
                EvaluationQuestion(
                    id = "q12",
                    number = 12,
                    text = "Pelayanan Learning Centre selama kegiatan berlangsung",
                    options = listOf(
                        "Sangat baik",
                        "Baik",
                        "Cukup",
                        "Kurang",
                        "Tidak baik"
                    )
                )
            )
        )
    )
}
