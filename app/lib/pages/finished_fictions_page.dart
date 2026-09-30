import 'package:app/main.dart';
import 'package:app/widgets/ficCard.dart';
import 'package:flutter/material.dart';

import '../models/fiction.dart';
import '../services/api_service.dart';

class FinishedFictionsPage extends StatefulWidget {
  const FinishedFictionsPage({super.key});

  @override
  State<FinishedFictionsPage> createState() => _FinishedFictionsPageState();
}

class _FinishedFictionsPageState extends State<FinishedFictionsPage> {
  final ApiService apiService = ApiService();

  late Future<List<Fiction>> fictions;

  @override
  void initState() {
    super.initState();
    fictions = apiService.getFinishedFictions();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<Fiction>>(
      future: fictions,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Scaffold(
            body: Center(
              child: CircularProgressIndicator(),
            ),
          );
        }

        if (snapshot.hasError) {
          return Scaffold(
            body: Center(
              child: Text('Error: ${snapshot.error}'),
            ),
          );
        }

        final fictions = snapshot.data ?? [];

        final numOfFictions = fictions.length;
        final totalWordCount = fictions.fold<int>(
          0,
          (sum, fiction) => sum + fiction.wordCount,
        );

        return Scaffold(
          appBar: AppBar(
            title: Row(
              spacing: 10,
              children: [
                Text('Finished Fictions: $numOfFictions'),
                Text('Total Word Count: $totalWordCount'),
              ],
            ),
          ),
          body: fictions.isEmpty
              ? const Center(
                  child: Text('No finished fictions.'),
                )
              : GridView.builder(
                  padding: const EdgeInsets.all(16),
                  gridDelegate:
                      const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 350,
                    childAspectRatio: 0.75,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                  ),
                  itemCount: fictions.length,
                  itemBuilder: (context, index) {
                    return FicCard(
                      fiction: fictions[index],
                      onTap: () {
                        print(
                          'Pressed ${fictions[index].title}',
                        );
                      },
                      onRemove: () async {
                        try {
                          await apiService.removeFiction(
                            fictions[index].id,
                          );

                          setState(() {
                            this.fictions =
                                apiService.getFinishedFictions();
                          });
                        } catch (e) {
                          print(e);
                        }
                      },
                    );
                  },
                ),
        );
      },
    );
  }
}
