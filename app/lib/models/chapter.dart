class Chapter {
  final String id;
  final String fictionId;
  final int chapterNumber;
  final String title;
  final String url;
  final String content;

  bool isRead(int readerProgress) {
    return chapterNumber <= readerProgress;
  }

  bool isUnread(int readerProgress) {
    return chapterNumber > readerProgress;
  }

  const Chapter({
    required this.id,
    required this.fictionId,
    required this.chapterNumber,
    required this.title,
    required this.url,
    required this.content,
  });

  factory Chapter.fromJson(Map<String, dynamic> json) {
    return Chapter(
      id: json['id'],
      fictionId: json['fictionId'],
      chapterNumber: json['chapterNumber'],
      title: json['title'],
      url: json['url'],
      content: json['content'],
    );
  }
}