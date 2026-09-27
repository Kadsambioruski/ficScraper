import 'package:app/models/chapter.dart';
import 'package:app/reader_page.dart';
import 'package:app/services/api_service.dart';
import 'package:flutter/material.dart';
import 'package:app/models/Fiction.dart';

void main() {
  runApp(const FicScraperApp());
}

class FicScraperApp extends StatelessWidget {
  const FicScraperApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: const FictionPage(),
    );
  }
}

class FictionPage extends StatefulWidget {
  const FictionPage({super.key});

  @override
  State<FictionPage> createState() => _FictionPageState();
}

class _FictionPageState extends State<FictionPage> {
  final ApiService apiService = ApiService();

  late Future<List<Fiction>> fictions;

  @override
  void initState() {
    super.initState();
    fictions = apiService.getFictions();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            IconButton(
              onPressed: () {},
              icon: const Icon(Icons.menu),
            ),
            const Text(
              'FicScraper',
              style: TextStyle(color: Colors.black),
            ),
          ],
        ),
        backgroundColor: Colors.grey,
      ),
      body: FutureBuilder<List<Fiction>>(
        future: fictions,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Error: ${snapshot.error}'),
            );
          }

          final fictions = snapshot.data ?? [];

          if (fictions.isEmpty) {
            return const Center(
              child: Text('No fictions found.'),
            );
          }

          return GridView.builder(
            padding: const EdgeInsets.all(10),
            itemCount: fictions.length,
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              crossAxisSpacing: 10,
              mainAxisSpacing: 10,
            ),
            itemBuilder: (context, index) {
              return FicCard(
                fiction: fictions[index],
                onTap: () {
                  print("Pressed ${fictions[index].title}");
                  Navigator.push(context, MaterialPageRoute(
                    builder: (context) => ChapterPage(
                      fictionId: fictions[index].id,
                      ),
                    ),
                  );
                },
                );
            },
          );
        },
      ),
    );
  }
}

class FicCard extends StatelessWidget {
  final Fiction fiction;
  final VoidCallback onTap;

  const FicCard({
    required this.fiction,
    required this.onTap,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: 200,
        padding: const EdgeInsets.all(20),
        margin: const EdgeInsets.only(right: 10),
        color: Colors.amber,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(fiction.title),
            Text(fiction.author),
            Text(fiction.site),
            Text('${fiction.chapAmount} chapters'),
            Text('${fiction.wordCount} words'),
          ],
        ),
      ),
    );
  }
}
class ChapterPage extends StatelessWidget {
  final String fictionId;

  const ChapterPage({
    required this.fictionId,
    super.key
  }); 

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Chapters"),
      ),
      body: FutureBuilder<List<Chapter>>(
        future: ApiService().getChapters(fictionId),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Error: ${snapshot.error}'),
            );
          }

          final chapters = snapshot.data ?? [];

          if (chapters.isEmpty) {
            return const Center(
              child: Text('No chapters found.'),
            );
          }

          return ListView.builder(
            itemCount: chapters.length,
            itemBuilder: (context, index) {
              final chapter = chapters[index];

              return ListTile(
                title: Text(chapter.title),
                subtitle: Text("Chapter ${chapter.chapterNumber}"),
                onTap: () {
                  Navigator.push(context, MaterialPageRoute(
                    builder: (context) => ReaderPage(
                      chapters: chapters,
                      currentIndex: index,
                      )
                    )
                  );
                },
              );
            },
          );
        },
      ),
    );
  }
}
